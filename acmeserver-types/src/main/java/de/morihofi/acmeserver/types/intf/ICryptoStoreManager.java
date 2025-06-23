package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import lombok.NonNull;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface ICryptoStoreManager {


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
     * Saves the keystore to the specified location, if it is a PKCS#12 keystore configuration.
     */
    void saveKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException;

    /**
     * Gets the intermediate certificate for a provisioner by its uuid
     *
     * @param uuid internal UUID of the provisioner
     * @return the X509 certificate of the provisioner's intermediate CA
     */
    X509Certificate getIntermediateCertificate(String uuid) throws KeyStoreException;

    /**
     * Gets the key pair for an intermediate CA by its uuid
     *
     * @param uuid internal UUID of the provisioner
     * @return the KeyPair of the provisioner's intermediate CA
     * @throws UnrecoverableKeyException if the key is unrecoverable
     * @throws KeyStoreException         if there is an issue with the keystore
     * @throws NoSuchAlgorithmException  if a required cryptographic algorithm is not available
     */
    KeyPair getIntermediateCertificateAuthorityKeyPair(@NonNull String uuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException;

    /**
     * Retrieves the full intermediate certificate chain for a provisioner by its internal UUID.
     *
     * @param internalUuid The internal UUID of the provisioner.
     * @return An array of X509 certificates representing the full intermediate certificate chain.
     */
    X509Certificate[] getFullIntermediateCertificateChain(String internalUuid) throws KeyStoreException;

    /**
     * Gets the name of the keystore provider.
     *
     * @return The name of the keystore provider.
     */
    String getKeyStoreProviderName();

    /**
     * Gets the SSLContext for the server using the specified server certificate uuid.
     *
     * @param uuid The internal UUID of the server certificate.
     * @return The SSLContext configured for the server.
     */
    SSLContext getSslContextForServer(String uuid) throws IOException;

    /**
     * Adds a new certificate authority to the keystore.
     *
     * @param rootCaEntity  The root certificate authority entity.
     * @param caKeyPair     The key pair for the certificate authority.
     * @param caCertificate The X509 certificate for the certificate authority.
     */
    void addCertificateAuthority(RootCa rootCaEntity, KeyPair caKeyPair, X509Certificate caCertificate) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException;

    /**
     * Adds a timestamp authority certificate to the keystore.
     *
     * @param certificateChain The X509 certificate chain for the timestamp authority.
     * @param kp               The key pair for the timestamp authority.
     * @param internalUuid     The internal UUID of the timestamp authority.
     */
    void addTimestampAuthority(X509Certificate[] certificateChain, KeyPair kp, String internalUuid) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException;

    /**
     * Adds an intermediate certificate authority to the keystore.
     *
     * @param intermediateCertificateChain The X509 certificate chain for the intermediate certificate authority.
     * @param intermediateKeyPair          The key pair for the intermediate certificate authority.
     * @param internalUuid                 The internal UUID of the intermediate certificate authority.
     */
    void addIntermediateCertificateAuthority(X509Certificate[] intermediateCertificateChain, KeyPair intermediateKeyPair, String internalUuid) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException;

    /**
     * Removes an intermediate certificate authority from the keystore.
     *
     * @param intermediateAlias The uuid of the intermediate certificate authority to remove.
     */
    void removeIntermediateCaCertificate(String intermediateAlias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException;

    /**
     * Retrieves the timestamp authority certificate by its internal UUID.
     *
     * @param internalUuid The internal UUID of the timestamp authority.
     * @return The X509 certificate for the timestamp authority.
     */
    X509Certificate getTimestampAuthorityCertificate(String internalUuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException;

    /**
     * Retrieves the key pair for the timestamp authority by its internal UUID.
     *
     * @param internalUuid The internal UUID of the timestamp authority.
     * @return The key pair for the timestamp authority.
     */
    KeyPair getTimeampAuthorityKeyPair(String internalUuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException;

    /**
     * Adds a server certificate to the keystore.
     *
     * @param x509CertificateChain The X509 certificate chain for the server.
     * @param keyPair              The key pair for the server certificate.
     * @param uuid                 The internal UUID of the server certificate.
     */
    void addServerCertificate(X509Certificate[] x509CertificateChain, KeyPair keyPair, String uuid) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException;
}
