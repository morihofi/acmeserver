mod jvm;
mod helper;
mod jdkdiscover;

use std::collections::{HashMap, HashSet};
use std::{env, fs};
use std::fs::File;
use std::hash::Hash;
use std::io::Read;
use std::path::{Path, PathBuf};
use anyhow::Context;
use anyhow::Result;
use jni::JNIEnv;
use jni::objects::{JCharArray, JObject, JString, JValue, JValueOwned};
use tracing::{debug, error, info, warn};
use jni::sys::{jint, jboolean, JavaVMInitArgs, JavaVMOption, JNIEnv as JNIEnvRaw, JavaVM as JavaVMRaw, JNI_VERSION_1_8, jchar};
use crate::java;

pub fn install_into_java_installation(runtime_java_home: &Path, java_homes_to_patch: Vec<&Path>, storepass: Option<&str>, overwrite: bool, entries_to_add: &HashMap<String, Vec<u8>>) -> anyhow::Result<()> {

    let libjvm = jvm::find_libjvm(runtime_java_home);

    if libjvm.is_none() {
        warn!("Unable to find libjvm for {}, skipping", runtime_java_home.display());
        return Err(anyhow::anyhow!("Unable to find libjvm for {}", runtime_java_home.display()));
    }
    let libjvm = libjvm.unwrap();
    debug!("Found libjvm at {}", libjvm.display());


    unsafe {
        match (jvm::create_jvm_instance(&*libjvm, &[], &[])) {
            Ok(jvm_jni) => {
                debug!("Created new embedded Java Virtual Machine instance");
                let jvm = jvm_jni.0;
                let mut jni = jvm_jni.1;

                for current_java_home in java_homes_to_patch {

                    let cacerts = locate_cacerts(current_java_home)
                        .with_context(|| format!("Unable to find cacerts under {}", current_java_home.display()))?;
                    info!(path=%cacerts.display(), "Using cacerts keystore");

                    // Ensure writable before we start JVM work.
                    fs::OpenOptions::new()
                        .read(true)
                        .write(true)
                        .open(&cacerts)
                        .with_context(|| format!("cacerts not writable: {}", cacerts.display()))?;


                // --- Java: KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                let ks_cls = jni.find_class("java/security/KeyStore")?;
                let default_type = jni
                    .call_static_method(&ks_cls, "getDefaultType", "()Ljava/lang/String;", &[])?
                    .l()?;
                let ks = jni
                    .call_static_method(
                        ks_cls,
                        "getInstance",
                        "(Ljava/lang/String;)Ljava/security/KeyStore;",
                        &[JValue::Object(&default_type)],
                    )?
                    .l()?;

                // --- Open FileInputStream for cacerts
                let cacerts_str: JString = jni.new_string(cacerts.to_string_lossy().as_ref())?;
                let cacerts_fis = jni.new_object(
                    "java/io/FileInputStream",
                    "(Ljava/lang/String;)V",
                    &[JValue::Object(&JObject::from(cacerts_str))],
                )?;

                // --- Password char[]
                let pw = crate::java::helper::to_java_char_array(&jni, storepass.unwrap_or("changeit"))?;

                // --- ks.load(fis, pw)
                jni.call_method(
                    &ks,
                    "load",
                    "(Ljava/io/InputStream;[C)V",
                    &[JValue::Object(&cacerts_fis), JValue::Object(&*pw)],
                )?;

                for (alias, cert_bytes) in entries_to_add.into_iter() {


                // --- Check existing alias
                let alias_js: JString = jni.new_string(&alias)?;
                let exists = jni
                    .call_method(
                        &ks,
                        "containsAlias",
                        "(Ljava/lang/String;)Z",
                        &[JValue::Object(&JObject::from(alias_js))],
                    )?
                    .z()?;

                if exists && !overwrite {
                    warn!("Alias '{}' already exists; not overwriting", &alias);
                    // Close FIS to be neat
                    let _: JValueOwned = jni.call_method(cacerts_fis, "close", "()V", &[])?;
                    return Ok(());
                }

                // --- Build Certificate from bytes via CertificateFactory X.509
                let der = crate::helper::convert::normalize_cert_bytes(&*cert_bytes)?;
                let jbytes = jni.byte_array_from_slice(&der)?;
                let bais = jni.new_object(
                    "java/io/ByteArrayInputStream",
                    "([B)V",
                    &[JValue::Object(&JObject::from(jbytes))],
                )?;

                let cf = jni
                    .call_static_method(
                        "java/security/cert/CertificateFactory",
                        "getInstance",
                        "(Ljava/lang/String;)Ljava/security/cert/CertificateFactory;",
                        &[JValue::Object(&JObject::from(jni.new_string("X.509")?))],
                    )?
                    .l()?;
                let cert = jni
                    .call_method(
                        cf,
                        "generateCertificate",
                        "(Ljava/io/InputStream;)Ljava/security/cert/Certificate;",
                        &[JValue::Object(&bais)],
                    )?
                    .l()?;

                // --- If exists, delete before set (some providers require removing first)
                if exists && overwrite {
                    let _: JValueOwned = jni.call_method(
                        &ks,
                        "deleteEntry",
                        "(Ljava/lang/String;)V",
                        &[JValue::Object(&JObject::from(jni.new_string(&alias)?))],
                    )?;
                    info!("Deleted existing alias '{}'", &alias);
                }

                // --- ks.setCertificateEntry(alias, cert)
                let _: JValueOwned = jni.call_method(
                    &ks,
                    "setCertificateEntry",
                    "(Ljava/lang/String;Ljava/security/cert/Certificate;)V",
                    &[
                        JValue::Object(&JObject::from(jni.new_string(&alias)?)),
                        JValue::Object(&cert),
                    ],
                )?;


                // --- Store back to cacerts
                let fos = jni.new_object(
                    "java/io/FileOutputStream",
                    "(Ljava/lang/String;)V",
                    &[JValue::Object(&JObject::from(jni.new_string(
                        cacerts.to_string_lossy().as_ref(),
                    )?))],
                )?;

                let _: JValueOwned = jni.call_method(
                    &ks,
                    "store",
                    "(Ljava/io/OutputStream;[C)V",
                    &[JValue::Object(&fos), JValue::Object(&*helper::to_java_char_array(&jni, storepass.unwrap_or("changeit"))?)],
                )?;

                // --- Close stream
                let _: JValueOwned = jni.call_method(fos, "close", "()V", &[])?;

                info!("Inserted certificate '{}' into {}", alias, cacerts.display());


                }

                // Close stream for cacerts file
                let _: JValueOwned = jni.call_method(cacerts_fis, "close", "()V", &[])?;

                info!("All certificates inserted");
                }


                info!("Shutting down JVM");
                drop(jni);
                jvm.detach_current_thread();
                jvm.destroy()?;


                Ok(())

            }
            Err(err) => {
                warn!("Unable to create Java JVM instance: {}", err);
                Err(anyhow::anyhow!("Java JVM instance creation failed"))?
            }
        }

    }

}

/// Locate <JAVA_HOME>/lib/security/cacerts (JDK 9+) or <JAVA_HOME>/jre/lib/security/cacerts (JDK 8 and older)
fn locate_cacerts(java_home: &Path) -> Option<PathBuf> {
    let candidates = [
        java_home.join("lib/security/cacerts"),
        java_home.join("jre/lib/security/cacerts"),
        // Some distros relocate security files under "conf" (Java 12+ images)
        java_home.join("conf/security/cacerts"),
    ];
    for p in candidates {
        if p.exists() && p.is_file() {
            return Some(p);
        }
    }
    None
}

pub(crate) fn install_into_java_auto(
    overwrite: bool,
    scan_other_users: bool,
    raw_certs_to_add: &Vec<Vec<u8>>,
) -> anyhow::Result<()> {
    use std::{collections::HashSet, env, path::{Path, PathBuf}};
    use tracing::{debug, info, warn};

    info!("Enumerating Java installations ...");

    let mut java_set: HashSet<PathBuf> = HashSet::new();

    // in env
    java_set.extend(
        ["JAVA_HOME", "JDK_HOME", "JRE_HOME"]
            .iter()
            .filter_map(|&name| env::var(name).ok())
            .inspect(|v| debug!(path = %v, "found Java installation in environment"))
            .map(PathBuf::from),
    );

    // in extra folders
    for p in jdkdiscover::discover_extra_java_homes(scan_other_users) {
        debug!(path=%p.display(), "found Java (extra discovery)");
        java_set.insert(p);
    }

    if java_set.is_empty() {
        warn!("No Java installations have been found to patch");
        return Ok(());
    }

    info!("Determining writeable Java cacerts ...");
    let java_bufs: Vec<PathBuf> = java_set
        .into_iter()
        .filter(|home| {
            if let Some(cacerts) = jdkdiscover::writable_cacerts(home) {
                debug!(home=%home.display(), cacerts=%cacerts.display(), "Java installation with writable cacerts");
                true
            } else {
                false
            }
        })
        .collect();


    let mut cert_map: HashMap<String, Vec<u8>> = HashMap::new();

    for raw_cert_bytes in raw_certs_to_add.clone().iter() {

        let cert_bytes = crate::helper::convert::normalize_cert_bytes(&*raw_cert_bytes)?;
        let alias = crate::helper::certalias::alias_from_cert(&raw_cert_bytes, Some(50))?;
        info!("calculated alias: {}", alias);
        cert_map.insert(alias.parse()?, cert_bytes.clone());
    }

    let homes_to_patch: Vec<&Path> = java_bufs.iter().map(|p| p.as_path()).collect();

    let runtime_java_home: &Path = homes_to_patch[0];
    install_into_java_installation(
        runtime_java_home,
        homes_to_patch,
        None,
        overwrite,
        &cert_map,
    )?;

    Ok(())
}
