use anyhow::Context;
use jni::objects::{JCharArray, JObject};
use jni::sys::{jchar, JNIEnv};

/// Convert a Rust &str to a Java char[] ([C) for KeyStore passwords.
pub(crate) fn to_java_char_array<'a>(env: &jni::JNIEnv<'a>, s: &str) -> jni::errors::Result<JCharArray<'a>> {
    let units: Vec<jchar> = s.encode_utf16().collect();
    let arr = env.new_char_array(units.len() as i32)?;
    env.set_char_array_region(&arr, 0, &units)?;
    Ok(arr)
}