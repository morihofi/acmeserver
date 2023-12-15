package de.morihofi.acmeserver.config.keyStoreHelpers;

public class PKCS11KeyStoreParams extends KeyStoreParams{
    private String libraryLocation;
    private int slot;

    public String getLibraryLocation() {
        return libraryLocation;
    }

    public int getSlot() {
        return slot;
    }
}
