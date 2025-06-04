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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * and handling various keystore configurations. It supports both PKCS#11 and PKCS#12 keystore configurations. This class is like an advanced
 * layer around Java's Keystore impl.
 */
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class CryptoStoreManager implements ICryptoStoreManager {

    /**
     * Alias for the ACME API certificate in the keystore.
     */
    public static final String KEYSTORE_ALIAS_ACMEAPI = "serverAcmeApi";
    /**
     * Prefix for aliases of intermediate certificate authorities in the keystore.
     */
    @Deprecated
    public static final String KEYSTORE_ALIASPREFIX_INTERMEDIATECA = "intermediateCA_";

    public String getKeyStoreAliasForProvisionerIntermediate(String provisioner) {
        return KEYSTORE_ALIASPREFIX_INTERMEDIATECA + provisioner;
    }

    /**
     * Key store configuration, including type and parameters.
     */
    private final IKeyStoreConfig keyStoreConfig;

    @Getter
    private KeyStore keyStore;


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
                keyStore = PKCS11KeyStoreLoader.loadPKCS11Keystore(
                        pkcs11Config.getPassword(),
                        pkcs11Config.getSlot(),
                        libraryLocation
                );
            }
            case PKCS12KeyStoreConfig pkcs12Config -> {
                log.info("Using PKCS#12 KeyStore at {}", pkcs12Config.getPath().toAbsolutePath().toString());
                keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
                if (Files.exists(pkcs12Config.getPath())) {
                    log.info("KeyStore does exist, loading existing into memory");
                    try (InputStream is = Files.newInputStream(pkcs12Config.getPath())) {
                        keyStore.load(is, pkcs12Config.getPassword());
                    }
                } else {
                    log.info("KeyStore does not exist, creating new KeyStore");
                    keyStore.load(null, pkcs12Config.getPassword());
                }
            }
            default -> throw new IllegalArgumentException("Unsupported key store config type: " + keyStoreConfig.getClass());
        }

    }


    @NonNull
    public KeyPair getCerificateAuthorityKeyPair(@NonNull RootCa rootCa) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(rootCa.getInternalUuid(), keyStore);
    }


    @NonNull
    public X509Certificate getCerificateAuthorityX509Certificate(@NonNull RootCa rootCa) throws KeyStoreException {
        return (X509Certificate) getKeyStore().getCertificate(rootCa.getInternalUuid());
    }


    @NonNull
    public KeyPair getIntermediateCerificateAuthorityKeyPair(@NonNull String intermediateCaName) throws UnrecoverableKeyException, KeyStoreException,
            NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(getKeyStoreAliasForProvisionerIntermediate(intermediateCaName), keyStore);
    }

    @NonNull
    public X509Certificate getX509CertificateForProvisioner(@NonNull String provisionerName) throws KeyStoreException {
        return (X509Certificate) getKeyStore().getCertificate(getKeyStoreAliasForProvisionerIntermediate(provisionerName));
    }



    public void saveKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if (keyStoreConfig instanceof PKCS12KeyStoreConfig pkcs12Config) {
            try (OutputStream fos = Files.newOutputStream(pkcs12Config.getPath())) {
                keyStore.store(fos, pkcs12Config.getPassword());
            }
        }
        // Hint: PKCS#11 does not need to be saved. It happens automatically when you create/remove certificate entry in the store
    }

}
