package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import lombok.NonNull;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface ICryptoStoreManager {

    /**
     * Returns the alias for the root certificate authority in the keystore.
     *
     * @return The alias for the root certificate authority.
     */
    String getKeyStoreAliasForProvisionerIntermediate(String name);

    /**
     * The loaded keystore instance for cryptographic operations.
     * @return KeyStore instance
     */
    KeyStore getKeyStore();

    /**
     * Retrieves the key pair for the root certificate authority from the keystore.
     *
     * @return The key pair associated with the root certificate authority.
     * @throws UnrecoverableKeyException If the key is unrecoverable.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    KeyPair getCerificateAuthorityKeyPair(@NonNull RootCa rootCa) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException;

    /**
     * Retrieves the X509 certificate for the root certificate authority from the keystore.
     *
     * @param rootCa The root certificate authority.
     * @return The X509 certificate associated with the root certificate authority.
     * @throws KeyStoreException If there is an issue with the keystore.
     */
    X509Certificate getCerificateAuthorityX509Certificate(@NonNull RootCa rootCa) throws KeyStoreException;

    /**
     * Retrieves the key pair for an intermediate certificate authority from the keystore.
     *
     * @param intermediateCaName The name of the intermediate certificate authority.
     * @return The key pair associated with the intermediate certificate authority.
     * @throws UnrecoverableKeyException If the key is unrecoverable.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    KeyPair getIntermediateCerificateAuthorityKeyPair(@NonNull String intermediateCaName) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException;

    /**
     * Retrieves the X509 certificate for an intermediate certificate authority from the keystore.
     *
     * @param provisionerName The name of the intermediate certificate authority.
     * @return The X509 certificate associated with the intermediate certificate authority.
     * @throws KeyStoreException If there is an issue with the keystore.
     */
    X509Certificate getX509CertificateForProvisioner(@NonNull String provisionerName) throws KeyStoreException;

    /**
     * Returns the password for the keystore, if it is a PKCS#12 keystore configuration.
     * <p>
     * <strong>Note:</strong> This method is deprecated and should not be used in new code and will be removed in future versions.
     *
     * @return The password for the keystore as a char array.
     */
    @Deprecated(forRemoval = true)
    char[] getKeyStorePassword();

    /**
     * Saves the keystore to the specified location, if it is a PKCS#12 keystore configuration.
     */
    void saveKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException;
}
