package de.morihofi.acmeserver.config.keyStoreHelpers;

/**
 * Represents the parameters for a PKCS12 KeyStore.
 * This class extends {@link KeyStoreParams} to include parameters specific to PKCS12 KeyStores,
 * such as the location of the KeyStore file.
 */
public class PKCS12KeyStoreParams extends KeyStoreParams {
    private String location;

    /**
     * Constructs a new PKCS12KeyStoreParams object.
     * Sets the type of KeyStore to 'pkcs12'.
     */
    public PKCS12KeyStoreParams() {
        type = "pkcs12";
    }

    /**
     * Retrieves the location of the PKCS12 KeyStore file.
     * The location is a path to the KeyStore file on the filesystem.
     *
     * @return The KeyStore file location as a {@code String}.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location of the PKCS12 KeyStore file.
     *
     * @param location The KeyStore file location as a {@code String}.
     */
    public void setLocation(String location) {
        this.location = location;
    }
}
