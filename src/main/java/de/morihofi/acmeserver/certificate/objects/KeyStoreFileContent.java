package de.morihofi.acmeserver.certificate.objects;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * Represents the contents of a KeyStore file.
 * This class encapsulates a key pair, a certificate, and the alias used for the certificate within the KeyStore.
 */
public class KeyStoreFileContent {
    private final KeyPair keyPair;
    private final X509Certificate cert;
    private final String certificateAlias;


    /**
     * Constructs a new KeyStoreFileContent object with the specified key pair, certificate, and certificate alias.
     *
     * @param keyPair The {@link KeyPair} to be stored in the KeyStore.
     * @param cert The {@link X509Certificate} to be associated with the key pair.
     * @param certificateAlias The alias used to reference the certificate in the KeyStore.
     */
    public KeyStoreFileContent(KeyPair keyPair, X509Certificate cert, String certificateAlias) {
        this.keyPair = keyPair;
        this.cert = cert;
        this.certificateAlias = certificateAlias;
    }


    /**
     * Retrieves the key pair stored in the KeyStore file.
     *
     * @return The {@link KeyPair} associated with the certificate in the KeyStore.
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * Retrieves the certificate stored in the KeyStore file.
     *
     * @return The {@link X509Certificate} associated with the key pair in the KeyStore.
     */
    public X509Certificate getCert() {
        return cert;
    }

    /**
     * Retrieves the alias of the certificate stored in the KeyStore file.
     * The alias is used as a reference to the certificate within the KeyStore.
     *
     * @return The alias of the certificate as a {@code String}.
     */
    public String getCertificateAlias() {
        return certificateAlias;
    }
}
