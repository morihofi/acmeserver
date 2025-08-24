use anyhow::{bail, Context, Result};
use jni::objects::{JObject, JValue};
use jni::{JNIEnv, JavaVM};
use jni::sys::{
    jint, jboolean, JavaVMInitArgs, JavaVMOption, JNIEnv as JNIEnvRaw, JavaVM as JavaVMRaw,
    JNI_VERSION_1_8, JNIInvokeInterface_ , JNI_TRUE,
};
use libloading::Library;
use std::env;
use std::ffi::{c_void, CString, c_char};
use std::path::{Path, PathBuf};
use std::ptr;
use tracing::{debug, error, info, warn};

// Finds libjvm in common locations under JAVA_HOME or when given the full path.
pub(crate) fn find_libjvm(java_home_or_libjvm: &Path) -> Option<PathBuf> {
    debug!("Trying to locate the shared JVM library...");

    if !java_home_or_libjvm.exists() {
        return None;
    }

    if java_home_or_libjvm.is_file() {
        return Some(PathBuf::from(java_home_or_libjvm));
    }

    if java_home_or_libjvm.is_dir() {
        let base = Path::new(java_home_or_libjvm);

        let candidates = [
            // Windows
            "bin/server/jvm.dll",
            "jre/bin/server/jvm.dll", // very old JDKs
            // Linux
            "lib/server/libjvm.so",
            "lib/amd64/server/libjvm.so",
            // macOS
            "lib/server/libjvm.dylib",
        ];

        for rel in candidates {
            let p = base.join(rel);
            debug!(candidate = %p.display(), "Checking candidate JVM library");
            if p.exists() {
                info!(path = %p.display(), "Found JVM library candidate");
                return Some(p);
            }
        }
    }

    error!("Unable to find JVM library in JAVA_HOME");
    None
}

// Type of JNI_CreateJavaVM in jvm.dll/libjvm.{so,dylib}
type JniCreateJavaVM = unsafe extern "system" fn(
    *mut *mut JavaVMRaw,
    *mut *mut c_void, // actually JNIEnv**
    *mut JavaVMInitArgs,
) -> jint;

/// Create a JVM instance and return both the JavaVM and the initial JNIEnv.
/// IMPORTANT: keeps libjvm loaded for the lifetime of the process.
pub(crate) unsafe fn create_jvm_instance(
    lib_path: &Path,
    classpath: &[String],
    jvm_args: &[String],
) -> Result<(JavaVM, JNIEnv<'static>)> {
    info!(lib = %lib_path.display(), "Loading JVM library");

    // Load and intentionally leak libjvm so it is never unloaded while JVM is alive.
    let lib = Library::new(lib_path)
        .with_context(|| format!("dlopen failed for {}", lib_path.display()))?;
    let _lib_static: &'static Library = Box::leak(Box::new(lib));

    // Resolve JNI_CreateJavaVM
    let create: libloading::Symbol<JniCreateJavaVM> =
        _lib_static.get(b"JNI_CreateJavaVM\0").context("missing JNI_CreateJavaVM export")?;
    debug!("Resolved JNI_CreateJavaVM symbol");

    // Build JavaVMInitArgs
    let cp_sep = if cfg!(windows) { ";" } else { ":" };
    let cp_opt = format!("-Djava.class.path={}", classpath.join(cp_sep));
    debug!(classpath = %cp_opt, "Computed classpath option");

    let owned_cstrs: Vec<CString> = std::iter::once(cp_opt)
        .chain(jvm_args.iter().cloned())
        .map(|s| CString::new(s).expect("no interior NULs"))
        .collect();

    let mut options: Vec<JavaVMOption> = owned_cstrs
        .iter()
        .map(|s| JavaVMOption {
            optionString: s.as_ptr() as *mut c_char,
            extraInfo: ptr::null_mut(),
        })
        .collect();

    let mut init_args = JavaVMInitArgs {
        version: JNI_VERSION_1_8,
        nOptions: options.len() as jint,
        options: options.as_mut_ptr(),
        ignoreUnrecognized: JNI_TRUE as jboolean,
    };

    info!(options = init_args.nOptions, "Creating JVM instance");

    let mut jvm_ptr: *mut JavaVMRaw = ptr::null_mut();
    let mut env_void: *mut c_void = ptr::null_mut();
    let rc = create(&mut jvm_ptr, &mut env_void, &mut init_args);
    if rc != 0 || jvm_ptr.is_null() || env_void.is_null() {
        error!(rc, "JNI_CreateJavaVM failed");
        bail!("JNI_CreateJavaVM failed (rc={rc})");
    }

    info!("JVM successfully created");

    // Wrap the raw handles
    let jvm = JavaVM::from_raw(jvm_ptr).context("wrap JavaVM failed")?;
    let env = JNIEnv::from_raw(env_void as *mut JNIEnvRaw).context("wrap JNIEnv failed")?;

    debug!("Wrapped raw JavaVM and JNIEnv");

    Ok((jvm, env))
}
