use std::ffi::OsStr;
use std::os::windows::ffi::OsStrExt;
use windows::Win32::UI::Shell::*;
use windows::Win32::UI::WindowsAndMessaging::*;
use windows::core::PCWSTR;

fn to_wide(s: &str) -> Vec<u16> {
    OsStr::new(s)
        .encode_wide()
        .chain(std::iter::once(0))
        .collect()
}

pub fn message_box(title: &str, content: &str, flags: MESSAGEBOX_STYLE) -> MESSAGEBOX_RESULT {
    unsafe {
        MessageBoxW(
            None,
            PCWSTR(to_wide(content).as_ptr()),
            PCWSTR(to_wide(title).as_ptr()),
            flags,
        )
    }
}

pub fn confirm(title: &str, content: &str) -> bool {
    let flags = MB_YESNO | MB_ICONQUESTION;
    let res = message_box(title, content, flags);
    return match res {
        IDYES => true,
        IDNO => false,
        _ => false,
    };
}

pub fn open_link(url: &str) {
    unsafe {
        ShellExecuteW(
            None,
            PCWSTR(to_wide("open").as_ptr()),
            PCWSTR(to_wide(url).as_ptr()),
            None,
            None,
            SW_SHOWNORMAL,
        );
    }
}
