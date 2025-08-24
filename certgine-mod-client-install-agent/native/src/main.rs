use ::windows::Win32::UI::WindowsAndMessaging::{
    MB_ICONERROR, MB_ICONINFORMATION, MB_ICONQUESTION, MB_OK, MB_YESNO,
};
use anyhow::anyhow;
use base64::prelude::*;
use indicatif::ProgressBar;
use reqwest::blocking::{Client, ClientBuilder};
use rustls::client::WebPkiServerVerifier;
use rustls::crypto::{CryptoProvider, ring};
use rustls::pki_types::CertificateDer;
use rustls::{ClientConfig, RootCertStore, crypto};
use std::collections::HashMap;
use std::fs::File;
use std::io::Read;
use std::sync::Arc;
use tracing::field::debug;
use tracing::{debug, info};

mod config;
mod helper;
mod java;

#[cfg(windows)]
mod windows;

fn main() -> anyhow::Result<(), anyhow::Error> {
    tracing_subscriber::fmt::init();

    let config = config::read_config()?;
    let title = config
        .client
        .title
        .clone()
        .unwrap_or_else(|| "Certgine Native Agent".to_string());

    #[cfg(windows)]
    {
        if config.client.r#type == "gui" && !config.client.unattended_mode {
            if !windows::ui::confirm(
                &title,
                "This will install root certificates of your local Certgine PKI. Continue?"
            ) {
                return Ok(());
            }
        }
    }

    let pb = if !config.client.silent_mode {
        let pb = ProgressBar::new(4);
        pb.set_message("Initializing");
        Some(pb)
    } else {
        None
    };

    let res: anyhow::Result<()> = (|| {
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

        if let Some(pb) = &pb {
            pb.inc(1);
            pb.set_message("Building client");
        }

        ring::default_provider().install_default().unwrap();
        let net_client = helper::networking::build_client(&config)?;

        info!(
            "Trying to connect to Certgine instance at {}:{} using TLS: {}",
            config.service.host, config.service.port, config.service.use_tls
        );

        if let Some(pb) = &pb {
            pb.inc(1);
            pb.set_message("Downloading certificates");
        }

        let mut cert_vec: Vec<Vec<u8>> = Vec::new();
        let certgine_base_url: String = helper::networking::get_certgine_url(&config)?;

        if config.certificate_authority.source.eq("remote") {
            for remote_uuid_cert in config.certificate_authority.trust_anchors.clone() {
                let res = net_client
                    .get(format!(
                        "{certgine_base_url}/dl/rootca/{remote_uuid_cert}.der"
                    ))
                    .send()?;
                debug!(
                    "Got der encoded root certificate for uuid {:?}",
                    remote_uuid_cert
                );
                cert_vec.push(res.bytes()?.to_vec());
            }
        } else {
            return Err(anyhow!("Unsupported certificate authority source"));
        }

        if let Some(pb) = &pb {
            pb.inc(1);
            pb.set_message("Installing certificates");
        }

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

        Ok(())
    })();

    if let Some(pb) = &pb {
        pb.finish_with_message("Done");
    }

    if let Err(e) = res {
        #[cfg(windows)]
        {
            if config.client.r#type == "gui" && !config.client.silent_mode {
                windows::ui::message_box(&title, &format!("Error: {e}"), MB_OK | MB_ICONERROR);
            }
        }
        return Err(e);
    }

    #[cfg(windows)]
    {
        if config.client.r#type == "gui"
            && !config.client.silent_mode
            && !config.client.unattended_mode
        {
            windows::ui::message_box(
                &title,
                "All root certificates were successfully installed!",
                MB_OK | MB_ICONINFORMATION,
            );
        }
    }

    info!("All done!");
    Ok(())
}
