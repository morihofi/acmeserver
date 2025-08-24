use std::sync::Arc;
use std::time::Duration;
use base64::Engine;
use base64::write::StrConsumer;
use reqwest::blocking::{Client, ClientBuilder};
use rustls::pki_types::CertificateDer;
use rustls::{ClientConfig, RootCertStore};
use rustls::client::WebPkiServerVerifier;
use rustls::crypto::{ring, CryptoProvider};
use tracing::debug;
use crate::config::AgentConfig;

pub(crate) fn build_client(agent_config: &AgentConfig) -> Result<reqwest::blocking::Client, anyhow::Error> {
    // Create a RootCertStore and add your cert
    let mut root_store = RootCertStore::empty();

    debug!("Building TLS config ...");
    for root_cert_b64url in agent_config.security.allowed_roots.clone() {
        let root_cert_der = base64::prelude::BASE64_URL_SAFE.decode(root_cert_b64url)?;
        let root_cert = CertificateDer::from_slice(&root_cert_der);

        root_store
            .add(root_cert)
            .expect("failed to add bundled root cert");
    }

    // Build rustls config
    let config = ClientConfig::builder()
        //.with_root_certificates(root_store)
        .with_webpki_verifier(
            WebPkiServerVerifier::builder_with_provider(<Arc<RootCertStore>>::from(root_store), <Arc<CryptoProvider>>::from(ring::default_provider()))
                //.with_crls(...)
                .build()?)
        .with_no_client_auth();


    debug!("Creating HTTP client ....");

    // Build reqwest client with this TLS connector
    let client: Client = ClientBuilder::new()
        .use_preconfigured_tls(config)
        .timeout(Duration::from_secs(agent_config.service.timeout as u64))
        .user_agent("Mozilla/5.0 certgine_native_agent/0.1.0")
        .build()?;

    Ok(client)
}

pub(crate) fn get_certgine_url(config: &AgentConfig) -> Result<String, anyhow::Error> {
    let protocol = if config.service.use_tls { "https" } else { "http" };
    let url: String = format!("{protocol}://{}:{}", config.service.host, config.service.port);
    Ok(url)
}