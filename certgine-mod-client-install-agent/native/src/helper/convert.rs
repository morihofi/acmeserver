use anyhow::Context;

/// Best-effort PEM or DER detection -> DER bytes.
/// Accepts both raw DER and standard PEM with header/footer.
pub(crate) fn normalize_cert_bytes(cert: &[u8]) -> anyhow::Result<Vec<u8>> {
    // Quick check for PEM header
    const PEM_BEGIN: &str = "-----BEGIN CERTIFICATE-----";
    if cert.starts_with(PEM_BEGIN.as_bytes()) || std::str::from_utf8(cert).map(|s| s.contains(PEM_BEGIN)).unwrap_or(false) {
        // Simple PEM decoding: strip non-base64 and decode
        let txt = std::str::from_utf8(cert).context("certificate is not valid UTF-8 (PEM expected)")?;
        let b64: String = txt
            .lines()
            .filter(|l| !l.starts_with("-----"))
            .collect();
        let der = base64::decode(b64).context("failed to base64-decode PEM certificate")?;
        Ok(der)
    } else {
        Ok(cert.to_vec())
    }
}

