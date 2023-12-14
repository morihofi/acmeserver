package de.morihofi.acmeserver.tools.certificate.cryptoops;

import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.IKeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS12KeyStoreConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class CryptoStoreManager {
    public static final String KEYSTORE_ALIAS_ROOTCA = "rootCA";
    public static final String KEYSTORE_ALIASPREFIX_INTERMEDIATECA = "intermediateCA_";

    private IKeyStoreConfig keyStoreConfig;
    private KeyStore keyStore;

    public CryptoStoreManager(IKeyStoreConfig keyStoreConfig) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        this.keyStoreConfig = keyStoreConfig;

        if (keyStoreConfig instanceof PKCS11KeyStoreConfig) {
            PKCS11KeyStoreConfig pkcs11Config = (PKCS11KeyStoreConfig) keyStoreConfig;

            keyStore = PKCS11KeyStoreLoader.loadPKCS11Keystore(pkcs11Config.getPassword(), pkcs11Config.getSlot(), pkcs11Config.getLibraryPath().toAbsolutePath().toString());
        }
        if (keyStoreConfig instanceof PKCS12KeyStoreConfig) {
            PKCS12KeyStoreConfig pkcs12Config = (PKCS12KeyStoreConfig) keyStoreConfig;

            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(Files.newInputStream(pkcs12Config.getPath()), pkcs12Config.getPassword().toCharArray());
        }
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

}
