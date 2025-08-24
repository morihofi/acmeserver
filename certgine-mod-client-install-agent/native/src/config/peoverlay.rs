use std::{fs::File, io::{Read, Seek, SeekFrom}};
use std::path::Path;
const MAGIC: &[u8] = b"CGJOCFGv1"; // stands for CertGineJsonObjectConFiG Version 1

#[derive(thiserror::Error, Debug)]
pub enum OverlayError {
    #[error("No overlay found (Missing Magic)")]
    NotFound,
    #[error("Corrupted Overlay: {0}")]
    Corrupt(&'static str),
    #[error(transparent)]
    Io(#[from] std::io::Error),
}



/// Reads the configuration overlay. Detects signed executables (certificate table at the file end)
/// and treats the start of that table as the end of file for trailer scanning.
pub fn read_overlay<P: AsRef<Path>>(path: P) -> Result<Vec<u8>, OverlayError> {
    let mut f = File::open(path)?;
    let file_len = f.metadata()?.len();

    // If signed: use the security directory start as the scan end
    let scan_end = find_security_directory_start(&mut f)?.unwrap_or(file_len);

    let trailer_min = MAGIC.len() as u64 + 4 + 4; // magic + len + crc
    if scan_end < trailer_min {
        return Err(OverlayError::NotFound);
    }

    // Verify MAGIC marker (directly before scan_end)
    f.seek(SeekFrom::Start(scan_end - MAGIC.len() as u64))?;
    let mut magic = vec![0u8; MAGIC.len()];
    f.read_exact(&mut magic)?;
    if magic != MAGIC {
        return Err(OverlayError::NotFound);
    }

    // LEN (4 bytes LE) directly before MAGIC
    f.seek(SeekFrom::Start(scan_end - MAGIC.len() as u64 - 4))?;
    let len = read_u32_le(&mut f)? as u64;

    // CRC32 (4 bytes LE) before LEN
    f.seek(SeekFrom::Start(scan_end - MAGIC.len() as u64 - 4 - 4))?;
    let crc_stored = read_u32_le(&mut f)?;

    // Calculate JSON start (relative to scan_end, not file end)
    let json_start = scan_end
        .checked_sub(MAGIC.len() as u64 + 4 + 4)
        .and_then(|x| x.checked_sub(len))
        .ok_or(OverlayError::Corrupt("negative JSON start"))?;

    // Read configuration bytes
    f.seek(SeekFrom::Start(json_start))?;
    let mut config_bytes = vec![0u8; len as usize];
    f.read_exact(&mut config_bytes)?;

    // Validate CRC
    let mut hasher = crc32fast::Hasher::new();
    hasher.update(&config_bytes);
    let crc_calc = hasher.finalize();
    if crc_calc != crc_stored {
        return Err(OverlayError::Corrupt("CRC mismatch"));
    }

    Ok(config_bytes)
}

#[cfg(test)]
mod tests {
    use super::*;
    use flate2::{write::GzEncoder, read::GzDecoder, Compression};
    use std::io::{Write, Read};
    use tempfile::NamedTempFile;

    #[test]
    fn read_overlay_returns_gzip_data() {
        let json = r#"{"foo":"bar"}"#;
        let mut encoder = GzEncoder::new(Vec::new(), Compression::default());
        encoder.write_all(json.as_bytes()).unwrap();
        let compressed = encoder.finish().unwrap();

        let mut file = NamedTempFile::new().unwrap();
        file.write_all(&compressed).unwrap();
        let mut hasher = crc32fast::Hasher::new();
        hasher.update(&compressed);
        let crc = hasher.finalize();
        file.write_all(&crc.to_le_bytes()).unwrap();
        file.write_all(&(compressed.len() as u32).to_le_bytes()).unwrap();
        file.write_all(MAGIC).unwrap();
        file.flush().unwrap();

        let data = read_overlay(file.path()).unwrap();

        let mut decoder = GzDecoder::new(&data[..]);
        let mut decoded = String::new();
        decoder.read_to_string(&mut decoded).unwrap();
        assert_eq!(decoded, json);
    }
}

/// Determines the file offset (not RVA) of the Security Directory (certificate table),
/// if present. Returns `Ok(None)` if none exists or headers are invalid.
fn find_security_directory_start(f: &mut File) -> Result<Option<u64>, OverlayError> {
    // DOS header: e_lfanew at 0x3C
    let mut mz = [0u8; 2];
    f.seek(SeekFrom::Start(0))?;
    f.read_exact(&mut mz)?;
    if &mz != b"MZ" {
        // Not a PE file -> no certificate
        return Ok(None);
    }
    f.seek(SeekFrom::Start(0x3C))?;
    let e_lfanew = read_u32_le(f)? as u64;

    // NT headers signature
    f.seek(SeekFrom::Start(e_lfanew))?;
    let mut pe_sig = [0u8; 4];
    f.read_exact(&mut pe_sig)?;
    if &pe_sig != b"PE\0\0" {
        return Ok(None);
    }

    // COFF header (20 B)
    let mut coff = [0u8; 20];
    f.read_exact(&mut coff)?;
    // Offset 16..18 in COFF: SizeOfOptionalHeader (u16)
    let size_opt = u16::from_le_bytes([coff[16], coff[17]]) as usize;

    // Optional header (variable size)
    let mut opt = vec![0u8; size_opt];
    f.read_exact(&mut opt)?;
    if size_opt < 2 {
        return Ok(None);
    }
    let magic = u16::from_le_bytes([opt[0], opt[1]]); // 0x10B (PE32) or 0x20B (PE32+)

    // Start of data directories within the optional header
    let (dd_start, needed_before_dd) = match magic {
        0x10B => (96usize, 96usize),  // PE32: 28 + 68
        0x20B => (112usize, 112usize), // PE32+: 24 + 88
        _ => return Ok(None),
    };
    if size_opt < dd_start || size_opt < dd_start + 8 * 5 {
        return Ok(None);
    }

    // NumberOfRvaAndSizes resides directly before data directories
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
        return Ok(None); // no security entry
    }

    // Directory #4 = Security (file offset instead of RVA)
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

    // Sanity check: certificate table is usually located at the end of the file.
    // Return the start offset; the reader uses this as the scan end.
    Ok(Some(va))
}

fn read_u32_le(f: &mut File) -> Result<u32, std::io::Error> {
    let mut b = [0u8; 4];
    f.read_exact(&mut b)?;
    Ok(u32::from_le_bytes(b))
}
