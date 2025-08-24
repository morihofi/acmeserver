use std::fs;
use std::path::{Path, PathBuf};
use tracing::{debug, warn};

/// Ist dies ein JAVA_HOME? -> existiert bin/java(.exe)
fn looks_like_java_home(dir: &Path) -> bool {
    let java_bin = if cfg!(windows) {
        dir.join("bin").join("java.exe")
    } else {
        dir.join("bin").join("java")
    };
    java_bin.is_file()
}

/// Durchforstet einen Root-Pfad (nicht rekursiv tief), sammelt alle Unterordner,
/// die wie ein JAVA_HOME aussehen (z. B. .../jdk-17, .../jbr, .../Contents/Home).
fn scan_root_for_jdks(root: &Path) -> Vec<PathBuf> {
    let mut found = Vec::new();
    let entries = match fs::read_dir(root) {
        Ok(it) => it,
        Err(e) => {
            debug!(root=%root.display(), error=%e, "skip root (unreadable)");
            return found;
        }
    };

    for entry in entries.flatten() {
        let path = entry.path();

        if path.is_dir() {
            // macOS: /Library/Java/JavaVirtualMachines/<x>.jdk/Contents/Home
            #[cfg(target_os = "macos")]
            {
                let mac_home = path.join("Contents").join("Home");
                if looks_like_java_home(&mac_home) {
                    debug!(path=%mac_home.display(), "found Java (macOS bundle)");
                    found.push(mac_home);
                    continue;
                }
            }

            // Allgemein: direktes JDK/JBR-Verzeichnis
            if looks_like_java_home(&path) {
                debug!(path=%path.display(), "found Java");
                found.push(path);
            } else {
                // Ein Level tiefer schauen (z. B. ~/.jdks/<jdk> / sdkman/asdf Strukturen)
                if let Ok(sub) = fs::read_dir(&path) {
                    for s in sub.flatten() {
                        let p = s.path();
                        if p.is_dir() && looks_like_java_home(&p) {
                            debug!(path=%p.display(), "found Java (nested)");
                            found.push(p);
                        }
                        // macOS nested Variante (Falls root/.../foo.jdk/Contents/Home)
                        #[cfg(target_os = "macos")]
                        {
                            let mac_home = p.join("Contents").join("Home");
                            if mac_home.is_dir() && looks_like_java_home(&mac_home) {
                                debug!(path=%mac_home.display(), "found Java (macOS nested bundle)");
                                found.push(mac_home);
                            }
                        }
                    }
                }
            }
        }
    }
    found
}

/// Liefert zusätzliche JAVA_HOME-Pfade:
/// - IntelliJ/JetBrains (z. B. ~/.jdks, Toolbox-Installationen inkl. jbr)
/// - Systempfade (Linux /usr/lib/jvm, macOS /Library/..., Homebrew)
/// - Paketmanager/Tools (SDKMAN, asdf, Scoop, Chocolatey)
/// Wenn `include_all_users=true`, werden (sofern erlaubt) auch Profile anderer Nutzer durchsucht.
pub fn discover_extra_java_homes(include_all_users: bool) -> Vec<PathBuf> {
    let mut candidates: Vec<PathBuf> = Vec::new();

    // --- Benutzerbezogene Wurzeln (aktueller User)
    if let Some(home) = dirs::home_dir() {
        // IntelliJ heruntergeladene JDKs (IDE-Dialog): ~/.jdks bzw. C:\Users\<user>\.jdks
        candidates.push(home.join(".jdks"));

        // SDKMAN
        candidates.push(home.join(".sdkman").join("candidates").join("java"));

        // asdf
        candidates.push(home.join(".asdf").join("installs").join("java"));

        // Scoop (Windows)
        if cfg!(windows) {
            candidates.push(home.join("scoop").join("apps"));                 // root
            candidates.push(home.join("scoop").join("apps").join("temurin17-jdk"));
            candidates.push(home.join("scoop").join("apps").join("temurin21-jdk"));
        }

        // JetBrains Toolbox IDEs -> jbr unterhalb der IDE-Installation
        // z. B. Windows: %LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-U\ch-0\<build>\jbr
        if let Some(local_app) = dirs::data_local_dir() {
            candidates.push(local_app.join("JetBrains").join("Toolbox").join("apps"));
        }
        // Klassische IDE-Installationen (können jbr enthalten)
        #[cfg(windows)]
        {
            candidates.push(PathBuf::from(r"C:\Program Files\JetBrains"));
            candidates.push(PathBuf::from(r"C:\Program Files (x86)\JetBrains"));
        }
        #[cfg(target_os = "macos")]
        {
            candidates.push(PathBuf::from("/Applications")); // IntelliJ.app/Contents/jbr
        }
        #[cfg(target_os = "linux")]
        {
            // Toolbox default unter ~/.local/share/JetBrains/Toolbox/apps
            if let Some(data_home) = dirs::data_dir() {
                candidates.push(data_home.join("JetBrains").join("Toolbox").join("apps"));
            }
        }
    }

    // --- Systemweite Wurzeln
    #[cfg(windows)]
    {
        candidates.push(PathBuf::from(r"C:\Program Files\Java"));
        candidates.push(PathBuf::from(r"C:\Program Files\Eclipse Adoptium"));
        candidates.push(PathBuf::from(r"C:\Program Files\AdoptOpenJDK"));
        candidates.push(PathBuf::from(r"C:\Program Files\Zulu"));
        candidates.push(PathBuf::from(r"C:\Program Files\Microsoft\jdk"));
        // Chocolatey
        candidates.push(PathBuf::from(r"C:\ProgramData\chocolatey\lib"));
    }

    #[cfg(target_os = "linux")]
    {
        candidates.push(PathBuf::from("/usr/lib/jvm"));
        candidates.push(PathBuf::from("/usr/java"));
        // Snap/Flatpak selten relevant, daher weggelassen
        // Homebrew auf Linux:
        candidates.push(PathBuf::from("/home/linuxbrew/.linuxbrew/opt"));
    }

    #[cfg(target_os = "macos")]
    {
        candidates.push(PathBuf::from("/Library/Java/JavaVirtualMachines"));
        candidates.push(PathBuf::from("/System/Volumes/Data/Library/Java/JavaVirtualMachines"));
        // Homebrew (Intel/Apple Silicon)
        candidates.push(PathBuf::from("/usr/local/opt"));
        candidates.push(PathBuf::from("/opt/homebrew/opt"));
        // IntelliJ.app enthält meist eine JBR unter Contents/jbr
        candidates.push(PathBuf::from("/Applications"));
    }

    // --- Optional: andere Nutzerprofile (mit Rechten)
    if include_all_users {
        #[cfg(windows)]
        {
            let users_root = Path::new(r"C:\Users");
            if let Ok(entries) = fs::read_dir(users_root) {
                for e in entries.flatten() {
                    let p = e.path();
                    if !p.is_dir() { continue; }
                    let name = e.file_name().to_string_lossy().to_lowercase();
                    // offensichtliche Profilordner überspringen
                    if matches!(name.as_str(), "public" | "default" | "default user" | "all users") {
                        continue;
                    }
                    candidates.push(p.join(".jdks"));
                    candidates.push(p.join("scoop").join("apps"));
                    candidates.push(p.join("AppData").join("Local").join("JetBrains").join("Toolbox").join("apps"));
                }
            } else {
                warn!("Could not enumerate C:\\Users (permission?)");
            }
        }
        #[cfg(any(target_os = "linux", target_os = "macos"))]
        {
            let users_root = Path::new("/home");
            if let Ok(entries) = fs::read_dir(users_root) {
                for e in entries.flatten() {
                    let p = e.path();
                    if !p.is_dir() { continue; }
                    candidates.push(p.join(".jdks"));
                    candidates.push(p.join(".sdkman").join("candidates").join("java"));
                    candidates.push(p.join(".asdf").join("installs").join("java"));
                    // Toolbox default auf Linux
                    candidates.push(p.join(".local").join("share").join("JetBrains").join("Toolbox").join("apps"));
                }
            } else {
                debug!("No /home listing (permission?)");
            }
            // macOS: Benutzer unter /Users
            #[cfg(target_os = "macos")]
            {
                let users_root = Path::new("/Users");
                if let Ok(entries) = fs::read_dir(users_root) {
                    for e in entries.flatten() {
                        let p = e.path();
                        if !p.is_dir() { continue; }
                        candidates.push(p.join(".jdks"));
                        candidates.push(p.join(".sdkman").join("candidates").join("java"));
                        candidates.push(p.join(".asdf").join("installs").join("java"));
                        candidates.push(p.join("Library").join("Application Support").join("JetBrains").join("Toolbox").join("apps"));
                    }
                }
            }
        }
    }

    // --- Scannen & Deduplizieren
    let mut results: Vec<PathBuf> = Vec::new();
    let mut seen = std::collections::HashSet::new();

    for root in candidates {
        if !root.exists() || !root.is_dir() {
            continue;
        }
        for home in scan_root_for_jdks(&root) {
            // Für JetBrains IDEs: oft liegt die JBR direkt unter <IDE>/jbr
            // Falls wir eine IDE-Installation treffen, nimm ggf. den jbr-Ordner selbst.
            let maybe_jbr = home.join("jbr");
            let final_home = if maybe_jbr.is_dir() && looks_like_java_home(&maybe_jbr) {
                maybe_jbr
            } else {
                home
            };

            let norm = final_home.clone(); // (optional: canonicalize, aber langsam/permission-heavy)
            if seen.insert(norm.clone()) {
                results.push(norm);
            }
        }
    }

    results
}



/// Prüft, ob unterhalb eines JAVA_HOME ein `cacerts` existiert und beschreibbar ist.
/// Gibt `Some(cacerts_path)` zurück, sonst `None`.
pub(crate) fn writable_cacerts(java_home: &Path) -> Option<PathBuf> {
    let candidates = [
        java_home.join("lib/security/cacerts"),
        java_home.join("jre/lib/security/cacerts"),
        java_home.join("conf/security/cacerts"),
    ];
    for c in candidates {
        if c.is_file() {
            // Schreibprobe: RW-OpenOptions
            if fs::OpenOptions::new().read(true).write(true).open(&c).is_ok() {
                return Some(c);
            } else {
                warn!(path=%c.display(), "cacerts exists but not writable");
            }
        }
    }
    None
}