package de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig;

import java.nio.file.Path;

/**
 * Represents a configuration for a PKCS#12 keystore, which is a file-based keystore typically stored in a .p12 file. This configuration
 * includes the file path and the password required to access the keystore.
 */
public class PKCS12KeyStoreConfig implements IKeyStoreConfig {

    /**
     * The password required to access the PKCS#12 keystore.
     */
    private String password;

    /**
     * The file system path to the PKCS#12 keystore file (.p12).
     */
    private Path path;

    /**
     * Constructs a new PKCS12KeyStoreConfig with the specified file path and password.
     *
     * @param path     The file system path to the PKCS#12 keystore file.
     * @param password The password required for accessing the keystore.
     */
    public PKCS12KeyStoreConfig(Path path, String password) {
        this.password = password;
        this.path = path;
    }

    /**
     * Retrieves the password required to access the PKCS#12 keystore.
     *
     * @return The password as a String.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password required to access the PKCS#12 keystore.
     *
     * @param password The new password as a String.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Retrieves the file system path to the PKCS#12 keystore file.
     *
     * @return The file path as a Path.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Sets the file system path to the PKCS#12 keystore file.
     *
     * @param path The new file path as a Path.
     */
    public void setPath(Path path) {
        this.path = path;
    }
}
