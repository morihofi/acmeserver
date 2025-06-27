package de.morihofi.certgine.cryptography.keystore;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreUtilTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509Certificate selfSigned(KeyPair kp) throws Exception {
        X500Name name = new X500Name("CN=test");
        Date now = new Date();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE, now, new Date(now.getTime() + 10000), name, kp.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);
    }

    @Test
    @DisplayName("inferTypeFromFile detects file extension")
    void testInferTypeFromFile() {
        assertEquals("JKS", KeyStoreUtil.inferTypeFromFile("test.jks"));
        assertEquals("PKCS12", KeyStoreUtil.inferTypeFromFile("file.p12"));
        assertEquals("PKCS11", KeyStoreUtil.inferTypeFromFile(null));
    }

    @Test
    @DisplayName("getKeyPair retrieves pair from keystore")
    void testGetKeyPair() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
        ks.load(null, null);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        X509Certificate cert = selfSigned(kp);
        ks.setKeyEntry("alias", kp.getPrivate(), "".toCharArray(), new java.security.cert.Certificate[]{cert});

        KeyPair loaded = KeyStoreUtil.getKeyPair("alias", ks);
        assertNotNull(loaded.getPrivate());
    }
}
