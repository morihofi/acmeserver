package de.morihofi.acmeserver.config.keyStoreHelpers;

public class PKCS11KeyStoreParams extends KeyStoreParams{
    private String libraryLocation;
    private int slot;

    public PKCS11KeyStoreParams(){
        type = "pkcs11";
    }

    public String getLibraryLocation() {
        return libraryLocation;
    }

    public int getSlot() {
        return slot;
    }

    public void setLibraryLocation(String libraryLocation) {
        this.libraryLocation = libraryLocation;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }
}
