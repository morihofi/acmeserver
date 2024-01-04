package de.morihofi.acmeserver.config.keyStoreHelpers;

/**
 * Represents the parameters for a PKCS11 KeyStore.
 * This class extends {@link KeyStoreParams} to include parameters specific to PKCS11 KeyStores,
 * such as the library location and slot number.
 */
public class PKCS11KeyStoreParams extends KeyStoreParams{
    private String libraryLocation;
    private int slot;

    /**
     * Constructs a new PKCS11KeyStoreParams object.
     * Sets the type of KeyStore to 'pkcs11'.
     */
    public PKCS11KeyStoreParams() {
        type = "pkcs11";
    }

    /**
     * Retrieves the library location for the PKCS11 KeyStore.
     * The library location refers to the path of the PKCS11 library on the system.
     *
     * @return The library location as a {@code String}.
     */
    public String getLibraryLocation() {
        return libraryLocation;
    }

    /**
     * Retrieves the slot number used by the PKCS11 KeyStore.
     * The slot number specifies the slot of the PKCS11 token.
     *
     * @return The slot number as an {@code int}.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Sets the library location for the PKCS11 KeyStore.
     *
     * @param libraryLocation The library location as a {@code String}.
     */
    public void setLibraryLocation(String libraryLocation) {
        this.libraryLocation = libraryLocation;
    }

    /**
     * Sets the slot number for the PKCS11 KeyStore.
     *
     * @param slot The slot number as an {@code int}.
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }
}
