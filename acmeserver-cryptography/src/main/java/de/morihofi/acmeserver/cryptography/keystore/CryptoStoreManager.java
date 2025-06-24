/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.cryptography.keystore;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.cryptography.keystore.IKeyStoreConfig;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.utils.regex.ConfigCheck;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
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
     * Alias for the ACME API certificate in the keystore.
     */
    public static final String KEYSTORE_ALIASPREFIX_SERVER = "server_";

    /**
     * Prefix for aliases of intermediate certificate authorities in the keystore.
     */
    public static final String KEYSTORE_ALIASPREFIX_INTERMEDIATECA = "intermediateCA_";

    /**
     * Prefix for aliases of timestamp authority certificates in the keystore.
     */
    public static final String KEYSTORE_ALIASPREFIX_TSA = "tsa_";


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
        return KEYSTORE_ALIASPREFIX_TSA + uuid;
    }

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
     * Constructs a CryptoStoreManager with the specified key store configuration.
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
    public CryptoStoreManager(@NonNull IKeyStoreConfig keyStoreConfig) throws CertificateException, IOException, NoSuchAlgorithmException,
            KeyStoreException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, NoSuchProviderException {
        this.keyStoreConfig = keyStoreConfig;

        switch (keyStoreConfig) {
            case PKCS11KeyStoreConfig pkcs11Config -> {
                String libraryLocation = pkcs11Config.getLibraryPath().toAbsolutePath().toString();
                log.info("Using PKCS#11 KeyStore with native library at {} with slot {}", libraryLocation, pkcs11Config.getSlot());
                this.keyStorePassword = pkcs11Config.getPassword().clone();

                keyStore = PKCS11KeyStoreLoader.loadPKCS11Keystore(
                        keyStorePassword,
                        pkcs11Config.getSlot(),
                        libraryLocation
                );
            }
            case PKCS12KeyStoreConfig pkcs12Config -> {
                log.info("Using PKCS#12 KeyStore at {}", pkcs12Config.getPath().toAbsolutePath().toString());
                this.keyStorePassword = pkcs12Config.getPassword().clone();

                keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
                if (Files.exists(pkcs12Config.getPath())) {
                    log.info("KeyStore does exist, loading existing into memory");
                    try (InputStream is = Files.newInputStream(pkcs12Config.getPath())) {
                        keyStore.load(is, keyStorePassword);
                    }
                } else {
                    log.info("KeyStore does not exist, creating new KeyStore");
                    keyStore.load(null, keyStorePassword);
                }
            }
            default ->
                    throw new IllegalArgumentException("Unsupported key store config type: " + keyStoreConfig.getClass());
        }

        // we cannot wipe the password here, because we won't be able to save it later

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
    public KeyPair getCerificateAuthorityKeyPair(@NonNull RootCa rootCa) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
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
    public X509Certificate getCerificateAuthorityX509Certificate(@NonNull RootCa rootCa) throws KeyStoreException {
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
    @NonNull
    public KeyPair getIntermediateCerificateAuthorityKeyPair(@NonNull String uuid) throws UnrecoverableKeyException, KeyStoreException,
            NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid, keyStore);
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
        if (isAllZero(keyStorePassword)) {
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
    public X509Certificate getIntermediateCertificate(@NonNull String uuid) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid);
    }

    @Override
    public KeyPair getIntermediateCertificateAuthorityKeyPair(@NonNull String uuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid, keyStore);
    }

    @Override
    public X509Certificate[] getFullIntermediateCertificateChain(String internalUuid) throws KeyStoreException {
        return Stream.of(keyStore.getCertificateChain(KEYSTORE_ALIASPREFIX_INTERMEDIATECA + internalUuid))
                .filter(c -> c instanceof X509Certificate)
                .map(c -> (X509Certificate) c)
                .toArray(X509Certificate[]::new);
    }

    @Override
    public String getKeyStoreProviderName() {
        return keyStore.getProvider().getName();
    }

    @Override
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
                KEYSTORE_ALIASPREFIX_TSA + internalUuid,
                kp.getPrivate(),
                "".toCharArray(),
                certificateChain
        );
        saveKeystore();
    }

    @Override
    public void addIntermediateCertificateAuthority(X509Certificate[] intermediateCertificateChain, KeyPair intermediateKeyPair, String internalUuid) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        keyStore.setKeyEntry(
                KEYSTORE_ALIASPREFIX_INTERMEDIATECA + internalUuid,
                intermediateKeyPair.getPrivate(),
                "".toCharArray(),
                intermediateCertificateChain
        );
        saveKeystore();
    }

    @Override
    public void removeIntermediateCaCertificate(String internalUuid) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        keyStore.deleteEntry(KEYSTORE_ALIASPREFIX_INTERMEDIATECA + internalUuid);
        saveKeystore();
    }

    @Override
    public X509Certificate getTimestampAuthorityCertificate(String internalUuid) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(KEYSTORE_ALIASPREFIX_TSA + internalUuid);
    }

    @Override
    public KeyPair getTimeampAuthorityKeyPair(String internalUuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(KEYSTORE_ALIASPREFIX_TSA + internalUuid, keyStore);
    }

    @Override
    public void addServerCertificate(X509Certificate[] x509CertificateChain, KeyPair keyPair, String uuid) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        keyStore.setKeyEntry(uuid, keyPair.getPrivate(), "".toCharArray(), x509CertificateChain);
        saveKeystore();
    }

    @Override
    public boolean containsAlias(@NonNull String alias) throws KeyStoreException {
        return keyStore.containsAlias(alias);
    }

    @Override
    public X509Certificate getCertificate(@NonNull String alias) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    @Override
    public KeyPair getKeyPairForAlias(@NonNull String alias) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(alias, keyStore);
    }

    @Override
    public boolean containsCertificateAuthority(@NonNull RootCa rootCa) throws KeyStoreException {
        return keyStore.containsAlias(rootCa.getInternalUuid());
    }

    @Override
    public boolean containsIntermediateCaCertificate(@NonNull String uuid) throws KeyStoreException {
        return keyStore.containsAlias(KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid);
    }

    @Override
    public boolean containsTimestampAuthorityCertificate(@NonNull String uuid) throws KeyStoreException {
        return keyStore.containsAlias(KEYSTORE_ALIASPREFIX_TSA + uuid);
    }

    @Override
    public boolean containsServerCertificate(@NonNull String uuid) throws KeyStoreException {
        return keyStore.containsAlias(uuid);
    }

    @Override
    public X509Certificate getServerCertificate(@NonNull String uuid) throws KeyStoreException {
        return (X509Certificate) keyStore.getCertificate(uuid);
    }

    @Override
    public KeyPair getServerKeyPair(@NonNull String uuid) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(uuid, keyStore);
    }

    @Override
    public X509Certificate[] getFullServerCertificateChain(@NonNull String uuid) throws KeyStoreException {
        return Stream.of(keyStore.getCertificateChain(uuid))
                .filter(c -> c instanceof X509Certificate)
                .map(c -> (X509Certificate) c)
                .toArray(X509Certificate[]::new);
    }

    @Override
    @NonNull
    public String getKeyStoreAliasForProvisionerIntermediate(@NonNull String uuid) {
        return KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid;
    }

    private static boolean isAllZero(char[] array) {
        for (char c : array) {          // fast & allocation-free
            if (c != '\0') return false;
        }
        return true;
    }

}
