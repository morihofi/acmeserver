package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import lombok.NonNull;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface ICryptoStoreManager {
    String getKeyStoreAliasForProvisionerIntermediate(String name);
    KeyStore getKeyStore();
    KeyPair getCerificateAuthorityKeyPair(@NonNull RootCa rootCa) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException;
    X509Certificate getCerificateAuthorityX509Certificate(@NonNull RootCa rootCa) throws KeyStoreException;
    KeyPair getIntermediateCerificateAuthorityKeyPair(@NonNull String intermediateCaName) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException;
    X509Certificate getX509CertificateForProvisioner(@NonNull String intermediateCaName) throws KeyStoreException;
    @Deprecated
    char[] getKeyStorePassword();
    void saveKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException;
}
