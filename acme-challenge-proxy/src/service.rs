use crate::challenge::challenge_checker_server::ChallengeChecker;
use crate::challenge::{DnsChallengeRequest, HttpChallengeRequest, ChallengeResponse};
use tonic::{Request, Response, Status};

#[derive(Default)]
pub struct ChallengeProxyService;

#[tonic::async_trait]
impl ChallengeChecker for ChallengeProxyService {
    async fn check_http_challenge(
        &self,
        request: Request<HttpChallengeRequest>,
    ) -> Result<Response<ChallengeResponse>, Status> {
        let req = request.into_inner();
        let url = format!(
            "http://{}/.well-known/acme-challenge/{}",
            req.domain, req.token
        );
        let body = reqwest::get(&url)
            .await
            .map_err(|e| Status::internal(e.to_string()))?
            .text()
            .await
            .map_err(|e| Status::internal(e.to_string()))?;
        let ok = body.trim() == req.expected_value;
        Ok(Response::new(ChallengeResponse { ok, details: String::new() }))
    }

    async fn check_dns_challenge(
        &self,
        request: Request<DnsChallengeRequest>,
    ) -> Result<Response<ChallengeResponse>, Status> {
        use trust_dns_resolver::{TokioAsyncResolver, config::*};
        let req = request.into_inner();
        let name = format!("_acme-challenge.{}", req.domain);
        let resolver = TokioAsyncResolver::tokio(ResolverConfig::default(), ResolverOpts::default());
        let response = resolver
            .txt_lookup(name)
            .await
            .map_err(|e| Status::internal(e.to_string()))?;
        let mut ok = false;
        for txt in response.iter() {
            for r in txt.txt_data() {
                if let Ok(s) = std::str::from_utf8(r) {
                    if s == req.expected_value {
                        ok = true;
                        break;
                    }
                }
            }
        }
        Ok(Response::new(ChallengeResponse { ok, details: String::new() }))
}
}
