use windows::Win32::System::Diagnostics::Debug::{FormatMessageW, FORMAT_MESSAGE_FROM_SYSTEM, FORMAT_MESSAGE_IGNORE_INSERTS};

fn to_wide_null(s: &str) -> Vec<u16> {
    use std::os::windows::ffi::OsStrExt;
    std::ffi::OsStr::new(s).encode_wide().chain(std::iter::once(0)).collect()
}

pub(crate) fn win32_message(code: u32) -> String {
    unsafe {
        let mut buf: *mut u16 = std::ptr::null_mut();
        let flags = FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS;
        let len = FormatMessageW(flags, None, code, 0, std::mem::transmute(&mut buf), 0, None);
        if len == 0 || buf.is_null() { return format!("(no system message for 0x{code:08X})"); }
        let slice = std::slice::from_raw_parts(buf, len as usize);
        String::from_utf16_lossy(slice).trim().to_string()
    }
}