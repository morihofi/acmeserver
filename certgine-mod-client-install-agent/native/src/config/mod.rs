use std::env;
use serde::Deserialize;
use tracing::debug;

mod peoverlay;

#[derive(Debug, Clone, PartialEq, Deserialize)]
pub struct AgentConfig {
    #[serde(default = "default_version")]
    pub version: u32,
    pub client: Client,
    pub service: Service,
    pub certificate_authority: CertificateAuthority,
    pub security: Security,
}

fn default_version() -> u32 { 1 }

#[derive(Debug, Clone, PartialEq, Deserialize)]
pub struct Client {
    pub r#type: String,           // "type" is a Rust keyword; use raw identifier
    pub enforce_elevated: bool,
    pub unattended_mode: bool,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
pub struct Service {
    pub host: String,
    pub port: u16,
    pub use_tls: bool,
    pub timeout: u32,
    pub retry_attempts: u32,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
pub struct CertificateAuthority {
    pub source: String,
    pub trust_anchors: Vec<String>,
}

#[derive(Debug, Clone, PartialEq, Deserialize)]
pub struct Security {
    pub allowed_roots: Vec<String>,
}


pub fn read_config() -> Result<AgentConfig, anyhow::Error> {

    // Get current executable
    let current_exe = env::current_exe()?;

    if !current_exe.exists() {
        Err(anyhow::anyhow!("No such file or directory for current executable at '{:?}'", current_exe))?;
    }

    debug!("Reading configuration (using overlay) ...");
    let overlay_config = peoverlay::read_overlay(&current_exe)?;


    debug!("Parsing configuration ...");
    let config: AgentConfig = serde_json::from_str::<AgentConfig>(overlay_config.as_str())?;
    debug!("Configuration loaded!");

    Ok(config)

}