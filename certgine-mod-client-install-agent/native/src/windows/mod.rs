mod helper;

use windows::core::w;
use windows::Win32::Foundation::GetLastError;
use windows::Win32::Security::Cryptography::*;
use std::ffi::c_void;
use tracing::{info, error, debug};
use crate::windows::helper::win32_message;

unsafe fn add_to_store(hstore: HCERTSTORE, der: &[u8]) -> anyhow::Result<()> {
    debug!("Creating certificate context from DER bytes ({} bytes)", der.len());

    let pctx = CertCreateCertificateContext(
        X509_ASN_ENCODING | PKCS_7_ASN_ENCODING,
        der,
    );
    if pctx.is_null() {
        let err = GetLastError().0;
        error!("Failed to create certificate context: error code {err} - {}", win32_message(err));
        return Err(anyhow::anyhow!("CertCreateCertificateContext failed: {err}"));
    }

    debug!("Adding certificate context to the store (replace existing if present)");
    let mut added = std::mem::zeroed();
    let ok = CertAddCertificateContextToStore(
        Some(hstore),
        pctx,
        CERT_STORE_ADD_REPLACE_EXISTING,
        Some(&mut added),
    );

    // Release local context
    CertFreeCertificateContext(Some(pctx));

    if ok.is_ok() {
        info!("Certificate successfully added to the store");
        Ok(())
    } else {
        let err = GetLastError().0;
        error!("Failed to add certificate to the store: error code {err} - {}", win32_message(err));
        Err(anyhow::anyhow!("CertAddCertificateContextToStore failed: {err}"))
    }
}

#[derive(Debug)]
pub enum TargetStore {
    CurrentUserRoot,
    LocalMachineRoot,
}

/// Import a Root CA into the selected Windows certificate store.
/// Expects DER bytes (convert PEM to DER before calling).
pub fn import_root_ca(der_certlist: Vec<Vec<u8>>, target: TargetStore) -> anyhow::Result<()> {
    unsafe {
        info!("Opening target certificate store: {:?}", target);

        let hstore_res = match target {
            TargetStore::CurrentUserRoot => {
                debug!("Opening CurrentUser ROOT store");
                CertOpenSystemStoreW(Some(HCRYPTPROV_LEGACY::default()), w!("ROOT"))
            }
            TargetStore::LocalMachineRoot => {
                debug!("Opening LocalMachine ROOT store with CERT_STORE_OPEN_EXISTING_FLAG");
                let flags = CERT_OPEN_STORE_FLAGS(
                    CERT_SYSTEM_STORE_LOCAL_MACHINE | CERT_STORE_OPEN_EXISTING_FLAG.0
                );

                let pvpara = Some(w!("ROOT").as_ptr() as *const c_void);

                CertOpenStore(
                    CERT_STORE_PROV_SYSTEM_W,
                    CERT_QUERY_ENCODING_TYPE(0),
                    Some(HCRYPTPROV_LEGACY::default()),
                    flags,
                    pvpara,
                )
            }
        };

        if let Err(e) = hstore_res {
            let err = GetLastError().0;
            error!("Failed to open certificate store: {e} (Win32 error code {err} - {})", win32_message(err));
            return Err(anyhow::anyhow!("Store open failed: {e} / {err}"));
        }
        let hstore = hstore_res?;

        for der in der_certlist {
            info!("Adding certificate to {:?} store", target);
            let res = add_to_store(hstore, &der);

            if res.is_err() {
                error!("Failed to add certificate to store: {}", res.err().unwrap());
            }
        }

        debug!("Closing certificate store handle");
        let _ = CertCloseStore(Some(hstore), 0);

        Ok(())
    }
}


