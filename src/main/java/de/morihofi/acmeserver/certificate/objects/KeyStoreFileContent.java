package de.morihofi.acmeserver.certificate.objects;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class KeyStoreFileContent {
    private KeyPair keyPair;
    private X509Certificate cert;
    private String certificateAlias;

    public KeyStoreFileContent(KeyPair keyPair, X509Certificate cert, String certificateAlias) {
        this.keyPair = keyPair;
        this.cert = cert;
        this.certificateAlias = certificateAlias;
    }


    public KeyPair getKeyPair() {
        return keyPair;
    }

    public X509Certificate getCert() {
        return cert;
    }

    public String getCertificateAlias() {
        return certificateAlias;
    }
}
