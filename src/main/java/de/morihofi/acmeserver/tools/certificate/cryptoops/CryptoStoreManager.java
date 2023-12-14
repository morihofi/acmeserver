package de.morihofi.acmeserver.tools.certificate.cryptoops;

import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.IKeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS12KeyStoreConfig;
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

    public static final String getKeyStoreAliasForProvisionerIntermediate(String provisioner){
        return KEYSTORE_ALIASPREFIX_INTERMEDIATECA + provisioner;
    }

    private IKeyStoreConfig keyStoreConfig;
    private KeyStore keyStore;

    public CryptoStoreManager(IKeyStoreConfig keyStoreConfig) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, NoSuchProviderException {
        this.keyStoreConfig = keyStoreConfig;

        if (keyStoreConfig instanceof PKCS11KeyStoreConfig) {
            PKCS11KeyStoreConfig pkcs11Config = (PKCS11KeyStoreConfig) keyStoreConfig;

            keyStore = PKCS11KeyStoreLoader.loadPKCS11Keystore(pkcs11Config.getPassword(), pkcs11Config.getSlot(), pkcs11Config.getLibraryPath().toAbsolutePath().toString());
        }
        if (keyStoreConfig instanceof PKCS12KeyStoreConfig) {
            PKCS12KeyStoreConfig pkcs12Config = (PKCS12KeyStoreConfig) keyStoreConfig;

            keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
            if (Files.exists(pkcs12Config.getPath())) {
                // If the file exists, load the existing KeyStore
                try (InputStream is = Files.newInputStream(pkcs12Config.getPath())) {
                    keyStore.load(is, pkcs12Config.getPassword().toCharArray());
                }
            } else {
                // Otherwise, initialize a new KeyStore
                keyStore.load(null, pkcs12Config.getPassword().toCharArray());
            }


        }
    }

    public KeyPair getCerificateAuthorityKeyPair() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return getKeyPair(KEYSTORE_ALIAS_ROOTCA);
    }

    public KeyPair getIntermediateCerificateAuthorityKeyPair(String intermediateCa) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        return getKeyPair(getKeyStoreAliasForProvisionerIntermediate(intermediateCa));
    }

    private KeyPair getKeyPair(String alias) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        if(!keyStore.containsAlias(alias)){
            throw new IllegalArgumentException("Alias " + alias + " does not exist in KeyStore");
        }

        KeyPair keyPair = new KeyPair(
                keyStore.getCertificate(alias).getPublicKey(),
                (PrivateKey) keyStore.getKey(alias, "".toCharArray())
        );

        return keyPair;
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
