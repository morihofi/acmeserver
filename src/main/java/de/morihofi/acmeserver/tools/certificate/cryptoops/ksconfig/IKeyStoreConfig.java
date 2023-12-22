package de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig;

/**
 * Interface of an KeyStore configuration
 */
public interface IKeyStoreConfig {
    /**
     * Gets the password for the keystore configuration.
     *
     * @return The keystore password as a String.
     */
    String getPassword();
}
