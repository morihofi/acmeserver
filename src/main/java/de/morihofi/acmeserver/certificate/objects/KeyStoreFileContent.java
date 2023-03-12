package de.morihofi.acmeserver.certificate.objects;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class KeyStoreFileContent {
    private final KeyPair keyPair;
    private final X509Certificate cert;
    private final String certificateAlias;

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
