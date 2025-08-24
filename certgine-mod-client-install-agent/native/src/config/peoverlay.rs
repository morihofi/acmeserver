use serde::Deserialize;
use std::{collections::HashMap, fs::File, io::{Read, Seek, SeekFrom}, path::PathBuf};
use std::path::Path;
use crate::config::AgentConfig;
const MAGIC: &[u8] = b"CGJOCFGv1"; // stands for CertGineJsonObjectConFiG Version 1

#[derive(thiserror::Error, Debug)]
pub enum OverlayError {
    #[error("No overlay found (Missing Magic)")]
    NotFound,
    #[error("Corrupted Overlay: {0}")]
    Corrupt(&'static str),
    #[error(transparent)]
    Io(#[from] std::io::Error),
    #[error(transparent)]
    Utf8(#[from] std::string::FromUtf8Error),
}



/// Liest das JSON-Overlay. Erkennt signierte EXEs (Certificate Table am Dateiende)
/// und betrachtet deren Anfang als "Dateiende" für den Trailer-Scan.
pub fn read_overlay<P: AsRef<Path>>(path: P) -> Result<String, OverlayError> {
    let mut f = File::open(path)?;
    let file_len = f.metadata()?.len();

    // Falls signiert: Security-Directory-Start als "Scan-Ende" nutzen
    let scan_end = find_security_directory_start(&mut f)?.unwrap_or(file_len);

    let trailer_min = MAGIC.len() as u64 + 4 + 4; // magic + len + crc
    if scan_end < trailer_min {
        return Err(OverlayError::NotFound);
    }

    // MAGIC prüfen (liegt direkt vor scan_end)
    f.seek(SeekFrom::Start(scan_end - MAGIC.len() as u64))?;
    let mut magic = vec![0u8; MAGIC.len()];
    f.read_exact(&mut magic)?;
    if magic != MAGIC {
        return Err(OverlayError::NotFound);
    }

    // LEN (4B LE) direkt vor MAGIC
    f.seek(SeekFrom::Start(scan_end - MAGIC.len() as u64 - 4))?;
    let len = read_u32_le(&mut f)? as u64;

    // CRC32 (4B LE) vor LEN
    f.seek(SeekFrom::Start(scan_end - MAGIC.len() as u64 - 4 - 4))?;
    let crc_stored = read_u32_le(&mut f)?;

    // JSON-Start berechnen (relativ zu scan_end, nicht Dateiende!)
    let json_start = scan_end
        .checked_sub(MAGIC.len() as u64 + 4 + 4)
        .and_then(|x| x.checked_sub(len))
        .ok_or(OverlayError::Corrupt("negativer JSON-Start"))?;

    // JSON lesen
    f.seek(SeekFrom::Start(json_start))?;
    let mut json_bytes = vec![0u8; len as usize];
    f.read_exact(&mut json_bytes)?;

    // CRC prüfen
    let mut hasher = crc32fast::Hasher::new();
    hasher.update(&json_bytes);
    let crc_calc = hasher.finalize();
    if crc_calc != crc_stored {
        return Err(OverlayError::Corrupt("CRC stimmt nicht"));
    }

    Ok(String::from_utf8(json_bytes)?)
}

/// Ermittelt den Dateioffset (nicht RVA!) des Security Directory (Certificate Table),
/// falls vorhanden. Liefert `Ok(None)`, wenn keins existiert oder Header unplausibel sind.
fn find_security_directory_start(f: &mut File) -> Result<Option<u64>, OverlayError> {
    // --- DOS Header: e_lfanew bei 0x3C ---
    let mut mz = [0u8; 2];
    f.seek(SeekFrom::Start(0))?;
    f.read_exact(&mut mz)?;
    if &mz != b"MZ" {
        // Kein PE -> kein Zertifikat
        return Ok(None);
    }
    f.seek(SeekFrom::Start(0x3C))?;
    let e_lfanew = read_u32_le(f)? as u64;

    // --- NT Headers Signatur ---
    f.seek(SeekFrom::Start(e_lfanew))?;
    let mut pe_sig = [0u8; 4];
    f.read_exact(&mut pe_sig)?;
    if &pe_sig != b"PE\0\0" {
        return Ok(None);
    }

    // --- COFF Header (20 B) ---
    let mut coff = [0u8; 20];
    f.read_exact(&mut coff)?;
    // Offset 16..18 im COFF: SizeOfOptionalHeader (u16)
    let size_opt = u16::from_le_bytes([coff[16], coff[17]]) as usize;

    // --- Optional Header (variabel) ---
    let mut opt = vec![0u8; size_opt];
    f.read_exact(&mut opt)?;
    if size_opt < 2 {
        return Ok(None);
    }
    let magic = u16::from_le_bytes([opt[0], opt[1]]); // 0x10B (PE32) oder 0x20B (PE32+)

    // Start der Data Directories innerhalb des Optional Headers
    let (dd_start, needed_before_dd) = match magic {
        0x10B => (96usize, 96usize),  // PE32: 28 + 68
        0x20B => (112usize, 112usize), // PE32+: 24 + 88
        _ => return Ok(None),
    };
    if size_opt < dd_start || size_opt < dd_start + 8 * 5 {
        return Ok(None);
    }

    // NumberOfRvaAndSizes steht direkt vor Data Directories
    if dd_start < 4 {
        return Ok(None);
    }
    let num_dirs = u32::from_le_bytes([
        opt[dd_start - 4],
        opt[dd_start - 3],
        opt[dd_start - 2],
        opt[dd_start - 1],
    ]);
    if num_dirs < 5 {
        return Ok(None); // kein Security-Eintrag
    }

    // Directory #4 = Security (FileOffset statt RVA!)
    let off = dd_start + 8 * 4;
    let va = u32::from_le_bytes([opt[off], opt[off + 1], opt[off + 2], opt[off + 3]]) as u64;
    let sz = u32::from_le_bytes([
        opt[off + 4],
        opt[off + 5],
        opt[off + 6],
        opt[off + 7],
    ]) as u64;

    if va == 0 || sz == 0 {
        return Ok(None);
    }

    // Plausibilitätscheck: Certificate Table liegt i. d. R. am Dateiende.
    // Wir geben den Startoffset zurück; der Reader nutzt das als Scan-Ende.
    Ok(Some(va))
}

fn read_u32_le(f: &mut File) -> Result<u32, std::io::Error> {
    let mut b = [0u8; 4];
    f.read_exact(&mut b)?;
    Ok(u32::from_le_bytes(b))
}
