use base64::prelude::*;
use reqwest::blocking::{Client, ClientBuilder};
use rustls::client::WebPkiServerVerifier;
use rustls::pki_types::CertificateDer;
use rustls::{crypto, ClientConfig, RootCertStore};
use std::collections::HashMap;
use std::fs::File;
use std::io::Read;
use std::sync::Arc;
use anyhow::anyhow;
use rustls::crypto::{ring, CryptoProvider};
use tracing::{debug, info};
use tracing::field::debug;

mod config;
mod helper;
mod java;

#[cfg(windows)]
mod windows;

fn main() -> anyhow::Result<(), anyhow::Error> {
    tracing_subscriber::fmt::init();

    let config = config::read_config()?;

    let mut is_elevated_process = false;
    #[cfg(windows)]
    {
        use windows_elevate::{check_elevated, elevate};

        is_elevated_process = windows_elevate::check_elevated()?;

        debug!("Is privileged level process: {:?}", is_elevated_process);

        if (config.client.enforce_elevated && !is_elevated_process) {
            debug!("Enforce elevated process, relaunching");
            windows_elevate::elevate()?; // relaunch with admin permissions
            return Ok(()); // exit the unprivileged process
        }
    }


    info!("Initializing ...");
    // Build networking client
    ring::default_provider().install_default().unwrap();
    let net_client = helper::networking::build_client(&config)?;

    info!(
        "Trying to connect to Certgine instance at {}:{} using TLS: {}",
        config.service.host, config.service.port, config.service.use_tls
    );

    let mut cert_vec: Vec<Vec<u8>> = Vec::new();
    let certgine_base_url: String = helper::networking::get_certgine_url(&config)?;

    if config.certificate_authority.source.eq("remote") {
        for remote_uuid_cert in config.certificate_authority.trust_anchors {

            let res = net_client.get(format!("{certgine_base_url}/dl/rootca/{remote_uuid_cert}.der"))
                .send()?;

            debug!("Got der encoded root certificate for uuid {:?}", remote_uuid_cert);
            cert_vec.push(res.bytes()?.to_vec());

        }
    }else {
        return Err(anyhow!("Unsupported certificate authority source"));
    }

    info!("All certificates collected, now installing ...");

    info!("Installing into Java ...");
    java::install_into_java_auto(true, is_elevated_process, &cert_vec)?;

    #[cfg(windows)]
    {
        let target_store: windows::TargetStore = if is_elevated_process {
            windows::TargetStore::LocalMachineRoot
        } else {
            windows::TargetStore::CurrentUserRoot
        };

        info!(
            "Installing into Windows Certificate Store at {:?} ...",
            target_store
        );
        windows::import_root_ca(cert_vec, target_store)?;
    }


    info!("All done!");
    Ok(())
}
