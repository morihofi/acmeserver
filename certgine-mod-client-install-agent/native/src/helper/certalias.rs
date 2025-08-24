use deunicode::deunicode;
use sha1::{Digest, Sha1};
use x509_parser::extensions::ParsedExtension;
use x509_parser::prelude::*;
use x509_parser::oid_registry::OID_X509_COMMON_NAME;


/// Build Keystore alias from Certificate Name
pub fn alias_from_cert(cert_der_bytes: &[u8], max_len: Option<usize>) -> Result<String, anyhow::Error> {

    let (_, cert) = x509_parser::parse_x509_certificate(&cert_der_bytes)
        .map_err(|e| anyhow::anyhow!("Unable to parse X.509 certificate: {e:?}"))?;

    // 1) try CN
    if let Some(cn) = extract_cn(&cert) {
        return Ok(finalize_alias("cn", &cn, &cert_der_bytes, max_len));
    }

    // 2) SAN / dNSName
    if let Some(dns) = extract_dnsname_from_san(&cert) {
        return Ok(finalize_alias("dns", &dns, &cert_der_bytes, max_len));
    }

    // 3) Fallback: sha1 Fingerprint
    Ok(finalize_alias("sha1", "", &cert_der_bytes, max_len))
}


fn extract_cn(cert: &X509Certificate<'_>) -> Option<String> {
    for attr in cert.subject().iter_attributes() {
        if attr.attr_type().eq(&OID_X509_COMMON_NAME) {
            return if let Ok(s) = attr.as_str() {
                Some(s.to_string())
            } else {
                Some(format!("{:?}", attr.attr_value()))
            }
        }
    }
    None
}


/// Erstes dNSName aus SAN ziehen
fn extract_dnsname_from_san(cert: &X509Certificate<'_>) -> Option<String> {
    for ext in cert.tbs_certificate.extensions().iter() {
        if let ParsedExtension::SubjectAlternativeName(san) = &ext.parsed_extension() {
            for gn in san.general_names.iter() {
                if let GeneralName::DNSName(d) = gn {
                    return Some(d.to_string());
                }
            }
        }
    }
    None
}

/// Normalisieren + Präfix + optionaler Längen-Cutoff.
/// Wenn `prefix == "sha1"` und `raw_value` leer ist, wird der SHA-1 Fingerprint genutzt.
fn finalize_alias(prefix: &str, raw_value: &str, der: &[u8], max_len: Option<usize>) -> String {
    let core = if prefix == "sha1" && raw_value.is_empty() {
        let mut h = Sha1::new();
        h.update(der);
        let digest = h.finalize();
        format!("sha1_{}", hex::encode(digest))
    } else {
        let norm = normalize_for_alias(raw_value);
        format!("{prefix}_{norm}")
    };

    if let Some(limit) = max_len {
        truncate_safely(&core, limit)
    } else {
        core
    }
}

/// Alias-freundliche Normalisierung:
/// - deunicode (ä→ae, ß→ss, …)
/// - lower
/// - nur [a-z0-9_], alles andere → '_'
/// - mehrere '_' zu einem
/// - trim '_' am Anfang/Ende
fn normalize_for_alias(s: &str) -> String {
    let ascii = deunicode(s).to_lowercase();
    let mut out = String::with_capacity(ascii.len());
    let mut prev_us = false;

    for ch in ascii.chars() {
        let allowed = ch.is_ascii_lowercase() || ch.is_ascii_digit();
        if allowed {
            out.push(ch);
            prev_us = false;
        } else {
            if !prev_us {
                out.push('_');
                prev_us = true;
            }
        }
    }

    // trim underscores
    while out.ends_with('_') { out.pop(); }
    while out.starts_with('_') { out.remove(0); }
    out
}

/// Harte Kürzung, ohne halbe Prefixe o.ä.
fn truncate_safely(s: &str, max_len: usize) -> String {
    if s.len() <= max_len {
        return s.to_string();
    }
    let mut out = s.chars().take(max_len).collect::<String>();
    // schneide evtl. hängendes '_' am Ende ab
    while out.ends_with('_') { out.pop(); }
    out
}

mod hex {
    // kleine, schlanke Hex-Ausgabe ohne zusätzliche Abhängigkeit
    pub fn encode(bytes: impl AsRef<[u8]>) -> String {
        const HEX: &[u8; 16] = b"0123456789abcdef";
        let b = bytes.as_ref();
        let mut s = String::with_capacity(b.len() * 2);
        for &v in b {
            s.push(HEX[(v >> 4) as usize] as char);
            s.push(HEX[(v & 0x0f) as usize] as char);
        }
        s
    }
}
