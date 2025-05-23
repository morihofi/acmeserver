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

package de.morihofi.acmeserver.tools.certificate.cryptoops;

import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.IKeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS12KeyStoreConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * The CryptoStoreManager class manages cryptographic operations, including loading and saving key stores, providing access to key pairs,
 * and handling various keystore configurations. It supports both PKCS#11 and PKCS#12 keystore configurations.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class CryptoStoreManager {

    /**
     * Alias for the root certificate authority in the keystore.
     */
    public static final String KEYSTORE_ALIAS_ROOTCA = "rootCA";
    /**
     * Alias for the ACME API certificate in the keystore.
     */
    public static final String KEYSTORE_ALIAS_ACMEAPI = "serverAcmeApi";
    /**
     * Prefix for aliases of intermediate certificate authorities in the keystore.
     */
    public static final String KEYSTORE_ALIASPREFIX_INTERMEDIATECA = "intermediateCA_";
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    public static String getKeyStoreAliasForProvisionerIntermediate(String provisioner) {
        return KEYSTORE_ALIASPREFIX_INTERMEDIATECA + provisioner;
    }
    /**
     * Logger for logging messages and events.
     */
    public final Logger log = LogManager.getLogger(getClass());
    /**
     * Key store configuration, including type and parameters.
     */
    private final IKeyStoreConfig keyStoreConfig;
    /**
     * The loaded keystore instance for cryptographic operations.
     */
    private KeyStore keyStore;
    /**
     * Check if the ACME Server is running for the first time, so the user can upload its existing CA
     */
    private boolean firstRun = false;

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
    public CryptoStoreManager(IKeyStoreConfig keyStoreConfig) throws CertificateException, IOException, NoSuchAlgorithmException,
            KeyStoreException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, NoSuchProviderException {
        this.keyStoreConfig = keyStoreConfig;

        if (keyStoreConfig instanceof PKCS11KeyStoreConfig pkcs11Config) {
            String libraryLocation = pkcs11Config.getLibraryPath().toAbsolutePath().toString();

            log.info("Using PKCS#11 KeyStore with native library at {} with slot {}", libraryLocation, pkcs11Config.getSlot());
            keyStore = PKCS11KeyStoreLoader.loadPKCS11Keystore(pkcs11Config.getPassword(), pkcs11Config.getSlot(), libraryLocation);

            if (!keyStore.containsAlias(KEYSTORE_ALIAS_ROOTCA)) {
                firstRun = true;
            }
        }
        if (keyStoreConfig instanceof PKCS12KeyStoreConfig pkcs12Config) {
            log.info("Using PKCS#12 KeyStore at {}", pkcs12Config.getPath().toAbsolutePath().toString());
            keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
            if (Files.exists(pkcs12Config.getPath())) {
                // If the file exists, load the existing KeyStore
                log.info("KeyStore does exist, loading existing into memory");
                try (InputStream is = Files.newInputStream(pkcs12Config.getPath())) {
                    keyStore.load(is, pkcs12Config.getPassword().toCharArray());
                }
            } else {
                // Otherwise, initialize a new KeyStore
                log.info("KeyStore does not exist, creating new KeyStore");
                keyStore.load(null, pkcs12Config.getPassword().toCharArray());

                firstRun = true;
            }
        }

        if (firstRun) {
            log.info("KeyStore is used for the first time");
        }
    }

    /**
     * Retrieves the key pair for the root certificate authority from the keystore.
     *
     * @return The key pair associated with the root certificate authority.
     * @throws UnrecoverableKeyException If the key is unrecoverable.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    public KeyPair getCerificateAuthorityKeyPair() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(KEYSTORE_ALIAS_ROOTCA, keyStore);
    }

    /**
     * Retrieves the key pair for an intermediate certificate authority from the keystore.
     *
     * @param intermediateCaName The name of the intermediate certificate authority.
     * @return The key pair associated with the intermediate certificate authority.
     * @throws UnrecoverableKeyException If the key is unrecoverable.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    public KeyPair getIntermediateCerificateAuthorityKeyPair(String intermediateCaName) throws UnrecoverableKeyException, KeyStoreException,
            NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(getKeyStoreAliasForProvisionerIntermediate(intermediateCaName), keyStore);
    }

    public X509Certificate getX509CertificateForProvisioner(String intermediateCaName) throws KeyStoreException {
        return (X509Certificate) getKeyStore().getCertificate(getKeyStoreAliasForProvisionerIntermediate(intermediateCaName));
    }

    /**
     * Retrieves the loaded keystore for cryptographic operations.
     *
     * @return The loaded keystore.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * Retrieves the password associated with the keystore configuration.
     *
     * @return The password for the keystore.
     */
    public String getKeyStorePassword() {
        return keyStoreConfig.getPassword();
    }

    /**
     * Saves the keystore to the specified location, if it is a PKCS#12 keystore configuration.
     *
     * @throws IOException              If there is an I/O error.
     * @throws CertificateException     If there is an issue with certificates.
     * @throws KeyStoreException        If there is an issue with the keystore.
     * @throws NoSuchAlgorithmException If a required cryptographic algorithm is not available.
     */
    public void saveKeystore() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        if (keyStoreConfig instanceof PKCS12KeyStoreConfig pkcs12Config) {

            try (OutputStream fos = Files.newOutputStream(pkcs12Config.getPath())) {
                keyStore.store(fos, pkcs12Config.getPassword().toCharArray());
            }
        }
    }

    /**
     * Returns if the keystore is used the first time
     *
     * @return is running first time
     */
    public boolean isFirstRun() {
        return firstRun;
    }

    /**
     * Sets the first-run in the keystore manager to false, so you can't update
     */
    public void disableFirstRunFlag() {
        firstRun = false;
        log.info("KeyStore first time use has been disabled (if not been done before)");
    }
}
