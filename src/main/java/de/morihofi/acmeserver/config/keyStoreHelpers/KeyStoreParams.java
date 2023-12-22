package de.morihofi.acmeserver.config.keyStoreHelpers;

import java.io.Serializable;

/**
 * Abstract base class for KeyStore parameters.
 * This class provides a common structure for different types of KeyStore parameters,
 * allowing them to be serialized and managed in a unified way. Subclasses should
 * provide specific implementations and additional properties relevant to the particular
 * type of KeyStore.
 */
public abstract class KeyStoreParams implements Serializable {
    protected String type;
    protected String password;

    /**
     * Retrieves the password of the KeyStore.
     * The password is used to access the KeyStore and should be protected.
     *
     * @return The password of the KeyStore as a {@code String}.
     */
    public String getPassword(){
        return password;
    }

    /**
     * Sets the password for the KeyStore.
     * This method allows specifying a password to access the KeyStore.
     *
     * @param password The password for the KeyStore as a {@code String}.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
