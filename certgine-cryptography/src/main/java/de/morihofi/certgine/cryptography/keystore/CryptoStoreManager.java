/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.keystore;

import de.morihofi.certgine.types.cryptography.CryptoStoreManagerConstants;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.cryptography.keystore.IKeyStoreConfig;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

/**
 * The CryptoStoreManager class manages cryptographic operations, including loading and saving key stores, providing access to key pairs,
 * and handling various keystore configurations. It supports both PKCS#11 and PKCS#12 keystore configurations. This class is like an advanced
 * layer around Java's Keystore impl.
 */
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class CryptoStoreManager implements ICryptoStoreManager {


    /**
     * Key store configuration, including type and parameters.
     */
    private final IKeyStoreConfig keyStoreConfig;
    /**
     * Password for the key store, used for loading and saving the keystore. This will be cleared from the in-memory configuration after loading the keystore.
     */
    private final char[] keyStorePassword;
    /**
     * The loaded key store instance for cryptographic operations.
     */
    private final KeyStore keyStore;

    /**
     * Constructs a CryptoStoreManager with the specified key store configuration and loader.
     *
     * @param keyStoreConfig The key store configuration to use.
     * @throws CertificateException      If there is an issue with certificates.
     * @throws IOException               If there is an I/O error.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws ClassNotFoundException    If a required class is not found.
     * @throws InvocationTargetException If there is an issue with invoking a method.
     * @throws InstantiationException    If there is an issue with instantiating a class.
     * @throws IllegalAccessException    If there is an issue with accessing a class or method.
     * @throws NoSuchMethodException     If a required method is not found.
     * @throws NoSuchProviderException   If a cryptographic provider is not found.
     */
    public CryptoStoreManager(@NonNull IKeyStoreConfig keyStoreConfig,
                               @NonNull KeyStoreLoader keyStoreLoader) throws CertificateException, IOException,
            NoSuchAlgorithmException, KeyStoreException, ClassNotFoundException,
            InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, NoSuchProviderException {
        this.keyStoreConfig = keyStoreConfig;
        this.keyStorePassword = KeyStoreUtils.clonePassword(keyStoreConfig.getPassword());
        this.keyStore = keyStoreLoader.load();
    }

    /**
     * Convenience constructor creating the appropriate loader based on the configuration.
     *
     * @param keyStoreConfig keystore configuration
     */
    public CryptoStoreManager(@NonNull IKeyStoreConfig keyStoreConfig) throws CertificateException,
            IOException, NoSuchAlgorithmException, KeyStoreException, ClassNotFoundException,
            InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, NoSuchProviderException {
        this(keyStoreConfig, createLoader(keyStoreConfig));
    }

    private static KeyStoreLoader createLoader(IKeyStoreConfig config) {
        return switch (config) {
            case PKCS12KeyStoreConfig pkcs12 -> new Pkcs12KeyStoreLoader(pkcs12);
            case de.morihofi.certgine.types.cryptography.keystore.PKCS11KeyStoreConfig pkcs11 ->
                    new Pkcs11KeyStoreLoader(pkcs11);
            default -> throw new IllegalArgumentException("Unsupported key store config type: " + config.getClass());
        };
    }

    /**
     * Returns the key store alias for a timestamp authority certificate.
     * The alias is constructed by appending the UUID to the predefined prefix.
     * The return value is not null, but it is possible that the alias does not exist in the keystore.
     *
     * @param uuid The UUID of the timestamp authority.
     * @return The key store alias for the timestamp authority certificate.
     */
    @NonNull
    public String getKeyStoreAliasForTimestampAuthority(@NonNull String uuid) {
        return KeyStoreUtils.getKeyStoreAliasForTimestampAuthority(uuid);
    }

    /**
     * Returns the key pair from the keystore for the root certificate authority provided.
     * This method retrieves the key pair associated with the root CA's internal UUID.
     *
     * @param rootCa The root certificate authority.
     * @return The key pair consisting of the public and private keys for the root CA.
     * @throws UnrecoverableKeyException If the key is unrecoverable (e.g., due to an incorrect password).
     * @throws KeyStoreException         If there is an issue accessing the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    @NonNull
    @Override
    public KeyPair getCertificateAuthorityKeyPair(@NonNull RootCa rootCa) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(rootCa.getInternalUuid(), keyStore);
    }

    /**
     * Returns the X509 certificate from the keystore for the root certificate authority provided.
     * This method retrieves the certificate associated with the root CA's internal UUID.
     *
     * @param rootCa The root certificate authority.
     * @return The X509 certificate associated with the root CA.
     * @throws KeyStoreException If there is an issue accessing the keystore.
     */
    @NonNull
    @Override
    public X509Certificate getCertificateAuthorityX509Certificate(@NonNull RootCa rootCa) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(rootCa.getInternalUuid());
    }

    /**
     * Returns the key pair for an intermediate certificate authority from the keystore.
     * This method retrieves the key pair associated with the intermediate CA's name.
     *
     * @param uuid The uuid of the intermediate certificate authority.
     * @return key pair consisting of the public and private keys for the intermediate CA.
     * @throws UnrecoverableKeyException If the key is unrecoverable (e.g., due to an incorrect password).
     * @throws KeyStoreException         If there is an issue accessing the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    @Override
    @NonNull
    public KeyPair getIntermediateCertificateAuthorityKeyPair(@NonNull String uuid) throws UnrecoverableKeyException, KeyStoreException,
            NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid, keyStore);
    }

    /**
     * Saves the current state of the keystore to the configured file path.
     * This method is applicable for PKCS#12 keystores and does not apply to PKCS#11 keystores.
     *
     * @throws CertificateException     If there is an issue with certificates.
     * @throws KeyStoreException        If there is an issue accessing the keystore.
     * @throws IOException              If there is an I/O error.
     * @throws NoSuchAlgorithmException If a required cryptographic algorithm is not available.
     */
    public void saveKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if (KeyStoreUtils.isAllZero(keyStorePassword)) {
            throw new IllegalStateException("KeyStore password is empty. Cannot save keystore without a password.");
        }

        if (keyStoreConfig instanceof PKCS12KeyStoreConfig pkcs12Config) {
            try (OutputStream fos = Files.newOutputStream(pkcs12Config.getPath())) {
                keyStore.store(fos, keyStorePassword);
            }
        }
        // Hint: PKCS#11 does not need to be saved. It happens automatically when you create/remove certificate entry in the store
    }

    @Override
    /**
     * Retrieves the certificate of the specified intermediate certificate authority.
     *
     * @param uuid the internal UUID of the intermediate CA
     * @return the corresponding {@link X509Certificate}
     * @throws KeyStoreException if the certificate cannot be accessed
     */
    public X509Certificate getIntermediateCertificate(@NonNull String uuid) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid);
    }

    @Override
    /**
     * Returns the full certificate chain for the intermediate certificate authority.
     *
     * @param internalUuid the UUID of the intermediate CA
     * @return the full certificate chain, starting with the intermediate certificate
     * @throws KeyStoreException if the chain cannot be retrieved
     */
    public X509Certificate[] getFullIntermediateCertificateChain(String internalUuid) throws KeyStoreException {
        return Stream.of(keyStore.getCertificateChain(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + internalUuid))
                .filter(c -> c instanceof X509Certificate)
                .map(c -> (X509Certificate) c)
                .toArray(X509Certificate[]::new);
    }

    @Override
    /**
     * Returns the name of the underlying {@link KeyStore} provider.
     *
     * @return provider name
     */
    public String getKeyStoreProviderName() {
        return keyStore.getProvider().getName();
    }

    @Override
    /**
     * Creates an {@link SSLContext} initialized with the certificate and key for the given server.
     *
     * @param uuid internal UUID identifying the server certificate
     * @return initialized SSL context
     * @throws IOException if the context cannot be created
     */
    public SSLContext getSslContextForServer(String uuid) throws IOException {
        try {
            KeyPair keyPair = getServerKeyPair(uuid);
            X509Certificate[] chain = getFullServerCertificateChain(uuid);

            KeyStore ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
            ks.load(null, null);
            ks.setKeyEntry("srv", keyPair.getPrivate(), new char[0], chain);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
            kmf.init(ks, new char[0]);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(ks);

            SSLContext context = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to create SSLContext", e);
        }
    }

    @Override
    public void addCertificateAuthority(RootCa rootCaEntity, KeyPair caKeyPair, X509Certificate caCertificate) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        keyStore.setKeyEntry(
                rootCaEntity.getInternalUuid(),
                caKeyPair.getPrivate(),
                "".toCharArray(),
                new X509Certificate[]{
                        caCertificate
                }
        );
        saveKeystore();
    }

    @Override
    public void addTimestampAuthority(X509Certificate[] certificateChain, KeyPair kp, String internalUuid) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        keyStore.setKeyEntry(
                CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA + internalUuid,
                kp.getPrivate(),
                "".toCharArray(),
                certificateChain
        );
        saveKeystore();
    }

    @Override
    public void addIntermediateCertificateAuthority(X509Certificate[] intermediateCertificateChain, KeyPair intermediateKeyPair, String internalUuid) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        keyStore.setKeyEntry(
                CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + internalUuid,
                intermediateKeyPair.getPrivate(),
                "".toCharArray(),
                intermediateCertificateChain
        );
        saveKeystore();
    }

    @Override
    public void removeIntermediateCaCertificate(String internalUuid) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        keyStore.deleteEntry(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + internalUuid);
        saveKeystore();
    }

    @Override
    /**
     * Retrieves the certificate for the timestamp authority with the given identifier.
     *
     * @param internalUuid internal UUID of the timestamp authority
     * @return the timestamp authority certificate
     * @throws KeyStoreException if the certificate cannot be accessed
     */
    public X509Certificate getTimestampAuthorityCertificate(String internalUuid) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA + internalUuid);
    }

    @Override
    /**
     * Retrieves the key pair associated with the specified timestamp authority.
     *
     * @param internalUuid internal UUID of the timestamp authority
     * @return the corresponding {@link KeyPair}
     * @throws UnrecoverableKeyException if the key cannot be recovered
     * @throws KeyStoreException         if the keystore cannot be accessed
     * @throws NoSuchAlgorithmException  if the key algorithm is unsupported
     */
    public KeyPair getTimestampAuthorityKeyPair(String internalUuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA + internalUuid, keyStore);
    }

    @Override
    /**
     * Adds or replaces the certificate and key pair for a server.
     *
     * @param x509CertificateChain certificate chain for the server
     * @param keyPair              key pair belonging to the server
     * @param uuid                 internal UUID of the server
     * @throws KeyStoreException        if the keystore cannot be written
     * @throws CertificateException     if certificate handling fails
     * @throws IOException              if saving the keystore fails
     * @throws NoSuchAlgorithmException if required algorithms are unavailable
     */
    public void addServerCertificate(X509Certificate[] x509CertificateChain, KeyPair keyPair, String uuid) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        keyStore.setKeyEntry(uuid, keyPair.getPrivate(), "".toCharArray(), x509CertificateChain);
        saveKeystore();
    }

    @Override
    /**
     * Checks if the keystore contains the specified alias.
     *
     * @param alias alias to look up
     * @return {@code true} if the alias exists
     * @throws KeyStoreException if the keystore cannot be queried
     */
    public boolean containsAlias(@NonNull String alias) throws KeyStoreException {
        return keyStore.containsAlias(alias);
    }

    @Override
    /**
     * Retrieves the certificate for the given alias.
     *
     * @param alias alias of the certificate
     * @return the corresponding {@link X509Certificate}
     * @throws KeyStoreException if the certificate cannot be accessed
     */
    public X509Certificate getCertificate(@NonNull String alias) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    @Override
    /**
     * Retrieves the key pair associated with the given alias.
     *
     * @param alias alias of the key pair
     * @return the corresponding {@link KeyPair}
     * @throws UnrecoverableKeyException if the key cannot be recovered
     * @throws KeyStoreException         if the keystore cannot be accessed
     * @throws NoSuchAlgorithmException  if the key algorithm is unsupported
     */
    public KeyPair getKeyPairForAlias(@NonNull String alias) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(alias, keyStore);
    }

    @Override
    /**
     * Checks whether the given root CA already exists in the keystore.
     *
     * @param rootCa root CA entity
     * @return {@code true} if the CA exists
     * @throws KeyStoreException if the keystore cannot be queried
     */
    public boolean containsCertificateAuthority(@NonNull RootCa rootCa) throws KeyStoreException {
        return keyStore.containsAlias(rootCa.getInternalUuid());
    }

    @Override
    /**
     * Checks if the keystore contains an intermediate CA certificate for the given UUID.
     *
     * @param uuid internal UUID of the intermediate CA
     * @return {@code true} if present
     * @throws KeyStoreException if the keystore cannot be queried
     */
    public boolean containsIntermediateCaCertificate(@NonNull String uuid) throws KeyStoreException {
        return keyStore.containsAlias(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid);
    }

    @Override
    /**
     * Determines whether a timestamp authority certificate with the given UUID exists.
     *
     * @param uuid internal UUID of the timestamp authority
     * @return {@code true} if present
     * @throws KeyStoreException if the keystore cannot be queried
     */
    public boolean containsTimestampAuthorityCertificate(@NonNull String uuid) throws KeyStoreException {
        return keyStore.containsAlias(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA + uuid);
    }

    @Override
    /**
     * Checks whether a server certificate with the given UUID exists in the keystore.
     *
     * @param uuid internal UUID of the server
     * @return {@code true} if the certificate exists
     * @throws KeyStoreException if the keystore cannot be queried
     */
    public boolean containsServerCertificate(@NonNull String uuid) throws KeyStoreException {
        return keyStore.containsAlias(uuid);
    }

    @Override
    /**
     * Retrieves the server certificate for the given UUID.
     *
     * @param uuid internal UUID of the server
     * @return the server certificate
     * @throws KeyStoreException if the certificate cannot be accessed
     */
    public X509Certificate getServerCertificate(@NonNull String uuid) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(uuid);
    }

    @Override
    /**
     * Retrieves the key pair associated with the specified server.
     *
     * @param uuid internal UUID of the server
     * @return the server's {@link KeyPair}
     * @throws UnrecoverableKeyException if the key cannot be recovered
     * @throws KeyStoreException         if the keystore cannot be accessed
     * @throws NoSuchAlgorithmException  if the key algorithm is unsupported
     */
    public KeyPair getServerKeyPair(@NonNull String uuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(uuid, keyStore);
    }

    @Override
    /**
     * Retrieves the entire certificate chain for the specified server.
     *
     * @param uuid internal UUID of the server
     * @return array containing the certificate chain
     * @throws KeyStoreException if the chain cannot be retrieved
     */
    public X509Certificate[] getFullServerCertificateChain(@NonNull String uuid) throws KeyStoreException {
        return Stream.of(keyStore.getCertificateChain(uuid))
                .filter(c -> c instanceof X509Certificate)
                .map(c -> (X509Certificate) c)
                .toArray(X509Certificate[]::new);
    }

    @Override
    /**
     * Constructs the alias for a provisioner intermediate certificate based on its UUID.
     *
     * @param uuid internal UUID of the provisioner intermediate CA
     * @return the keystore alias for the provisioner intermediate certificate
     */
    @NonNull
    public String getKeyStoreAliasForProvisionerIntermediate(@NonNull String uuid) {
        return KeyStoreUtils.getKeyStoreAliasForProvisionerIntermediate(uuid);
    }

}
