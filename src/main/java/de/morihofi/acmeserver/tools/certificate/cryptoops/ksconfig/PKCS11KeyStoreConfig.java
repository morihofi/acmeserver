package de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig;

import java.nio.file.Path;

public class PKCS11KeyStoreConfig implements IKeyStoreConfig{
    private String pin;
    private Path libraryPath;
    private int slot;

    public PKCS11KeyStoreConfig(Path libraryPath, int slot, String pin) {
        this.pin = pin;
        this.libraryPath = libraryPath;
        this.slot = slot;
    }

    @Override
    public String getPassword() {
        return pin;
    }

    public void setPassword(String pin) {
        this.pin = pin;
    }

    public Path getLibraryPath() {
        return libraryPath;
    }

    public void setLibraryPath(Path libraryPath) {
        this.libraryPath = libraryPath;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }
}
