package de.morihofi.acmeserver.tools.certificate.cryptoops;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.IKeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS12KeyStoreConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.security.*;
import java.security.cert.CertificateException;

public class CryptoStoreManager {
    public static final String KEYSTORE_ALIAS_ROOTCA = "rootCA";
    public static final String KEYSTORE_ALIAS_ACMEAPI = "serverAcmeApi";
    public static final String KEYSTORE_ALIASPREFIX_INTERMEDIATECA = "intermediateCA_";

    public final Logger log = LogManager.getLogger(getClass());

    public static String getKeyStoreAliasForProvisionerIntermediate(String provisioner){
        return KEYSTORE_ALIASPREFIX_INTERMEDIATECA + provisioner;
    }

    private IKeyStoreConfig keyStoreConfig;
    private KeyStore keyStore;


    public CryptoStoreManager(IKeyStoreConfig keyStoreConfig) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, NoSuchProviderException {
        this.keyStoreConfig = keyStoreConfig;

        if (keyStoreConfig instanceof PKCS11KeyStoreConfig pkcs11Config) {
            String libraryLocation = pkcs11Config.getLibraryPath().toAbsolutePath().toString();

            log.info("Using PKCS#11 KeyStore with native library at \"" + libraryLocation + "\" with slot " + pkcs11Config.getSlot());
            keyStore = PKCS11KeyStoreLoader.loadPKCS11Keystore(pkcs11Config.getPassword(), pkcs11Config.getSlot(), libraryLocation);
        }
        if (keyStoreConfig instanceof PKCS12KeyStoreConfig pkcs12Config) {
            log.info("Using PKCS#12 KeyStore at \"" + pkcs12Config.getPath().toAbsolutePath().toString() + "\"");
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
            }
        }
    }

    public KeyPair getCerificateAuthorityKeyPair() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(KEYSTORE_ALIAS_ROOTCA, keyStore);
    }

    public KeyPair getIntermediateCerificateAuthorityKeyPair(String intermediateCaName) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return KeyStoreUtil.getKeyPair(getKeyStoreAliasForProvisionerIntermediate(intermediateCaName), keyStore);
    }


    public KeyStore getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePassword() {
        return keyStoreConfig.getPassword();
    }

    public void saveKeystore() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        if (keyStoreConfig instanceof PKCS12KeyStoreConfig) {
            PKCS12KeyStoreConfig pkcs12Config = (PKCS12KeyStoreConfig) keyStoreConfig;

            try (OutputStream fos = Files.newOutputStream(pkcs12Config.getPath())) {
                keyStore.store(fos, pkcs12Config.getPassword().toCharArray());
            }
        }
    }


}
