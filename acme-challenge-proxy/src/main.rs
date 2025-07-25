mod service;
mod challenge {
    tonic::include_proto!("acme.challenge.proxy");
}
use clap::Parser;
use service::ChallengeProxyService;
use std::net::{SocketAddr, TcpListener};
use tonic::transport::{Certificate, Identity, Server, ServerTlsConfig};

#[derive(Parser)]
struct Opts {
    #[clap(long, default_value = "0.0.0.0:50051")]
    addr: String,
    #[clap(long)]
    cert: String,
    #[clap(long)]
    key: String,
    #[clap(long)]
    client_ca: Option<String>,
}

fn network_capabilities() -> (bool, bool) {
    let ipv4 = TcpListener::bind("0.0.0.0:0").is_ok();
    let ipv6 = TcpListener::bind("[::]:0").is_ok();
    (ipv4, ipv6)
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let opts = Opts::parse();
    let addr: SocketAddr = opts.addr.parse()?;

    let cert = tokio::fs::read(opts.cert).await?;
    let key = tokio::fs::read(opts.key).await?;
    let identity = Identity::from_pem(cert, key);

    let mut tls = ServerTlsConfig::new().identity(identity);
    if let Some(ca_path) = opts.client_ca {
        let ca = tokio::fs::read(ca_path).await?;
        tls = tls.client_ca_root(Certificate::from_pem(ca));
    }

    let (ipv4, ipv6) = network_capabilities();
    println!("Registering service: IPv4={}, IPv6={}", ipv4, ipv6);

    let svc = ChallengeProxyService::default();

    Server::builder()
        .tls_config(tls)?
        .add_service(challenge::challenge_proxy_server::ChallengeProxyServer::new(svc))
        .serve(addr)
        .await?;

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::network_capabilities;
    #[test]
    fn network_caps() {
        let (v4, v6) = network_capabilities();
        assert!(v4 || v6);
    }
}
