package de.morihofi.acmeserver.cryptography.tsa;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeStampAuthorityTest {
    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509Certificate createCert(KeyPair kp) throws Exception {
        X500Name name = new X500Name("CN=TSA");
        Date now = new Date();
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                name, BigInteger.ONE, now, new Date(now.getTime() + 86400000L),
                name, SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded()));
        builder.addExtension(Extension.extendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(holder);
    }

    @Test
    @DisplayName("generate creates valid token")
    void testGenerate() throws Exception {
        KeyPair kp = de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = createCert(kp);
        TimeStampAuthority auth = new TimeStampAuthority(kp.getPrivate(), cert, List.of(cert), "1.3.6.1.4.1.13762.3");
        byte[] data = MessageDigest.getInstance("SHA-256").digest("hi".getBytes());
        TimeStampRequest req = new TimeStampRequestGenerator().generate(TSPAlgorithms.SHA256, data);
        byte[] respBytes = auth.generate(req);
        TimeStampResponse resp = new TimeStampResponse(respBytes);
        assertArrayEquals(data, resp.getTimeStampToken().getTimeStampInfo().getMessageImprintDigest());
    }
}
