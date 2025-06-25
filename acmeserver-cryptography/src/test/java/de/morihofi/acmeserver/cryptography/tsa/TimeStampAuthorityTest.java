package de.morihofi.acmeserver.cryptography.tsa;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static de.morihofi.acmeserver.cryptography.tsa.TimeStampAuthority.getHashAlgorithmName;
import static org.junit.jupiter.api.Assertions.*;

public class TimeStampAuthorityTest {

    private static PrivateKey privateKey;
    private static X509Certificate cert;
    private static List<X509Certificate> certChain;
    private static final String POLICY_OID = "1.3.6.1.4.1.13762.3";

    private static final Set<ASN1ObjectIdentifier> ALGORITHMS = Set.of(
            TSPAlgorithms.MD5,
            TSPAlgorithms.RIPEMD160,
            TSPAlgorithms.SHA1,
            TSPAlgorithms.SHA224,
            TSPAlgorithms.SHA256,
            TSPAlgorithms.SHA384,
            TSPAlgorithms.SHA512
    );

    @BeforeAll
    static void setup() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        privateKey = keyPair.getPrivate();

        // Dummy self-signed certificate für Tests (echtes TLS-Cert nicht notwendig)
        cert = generate("CN=Test TSA", keyPair);
        certChain = List.of(cert);
    }

    public static X509Certificate generate(String subjectDN, KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date start = new Date(now);
        Date end = new Date(now + (365L * 24 * 60 * 60 * 1000)); // 1 Jahr Gültigkeit

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                new org.bouncycastle.asn1.x500.X500Name(subjectDN),
                BigInteger.valueOf(now),
                start,
                end,
                new org.bouncycastle.asn1.x500.X500Name(subjectDN),
                keyPair.getPublic()
        );

        // Extensions: EKU → Timestamping
        KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature);
        certBuilder.addExtension(Extension.keyUsage, true, keyUsage);

        ExtendedKeyUsage eku = new ExtendedKeyUsage(new KeyPurposeId[] {
                KeyPurposeId.id_kp_timeStamping
        });
        certBuilder.addExtension(Extension.extendedKeyUsage, true, eku);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(signer));
    }

    @Test
    void testAllSupportedAlgorithms() throws Exception {
        TimeStampAuthority tsa = new TimeStampAuthority(privateKey, cert, certChain, POLICY_OID);

        for (ASN1ObjectIdentifier alg : ALGORITHMS) {
            TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
            reqGen.setCertReq(true);

            MessageDigest md = MessageDigest.getInstance(getHashAlgorithmName(alg), BouncyCastleProvider.PROVIDER_NAME);
            byte[] hashedMessage = md.digest("Testdaten".getBytes());
            TimeStampRequest req = reqGen.generate(alg, hashedMessage, BigInteger.ONE);


            byte[] responseBytes = tsa.generate(req);
            assertNotNull(responseBytes, "Response is null for alg " + alg.getId());

            TimeStampResponse response = new TimeStampResponse(responseBytes);
            response.validate(req);
            assertEquals(PKIStatus.GRANTED, response.getStatus(), "Status not GRANTED for alg " + alg.getId());

            TimeStampToken token = response.getTimeStampToken();
            assertNotNull(token, "No token returned for alg " + alg.getId());
        }
    }
}
