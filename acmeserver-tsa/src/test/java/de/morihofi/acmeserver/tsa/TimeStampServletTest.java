package de.morihofi.acmeserver.tsa;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import de.morihofi.acmeserver.cryptography.tsa.TimeStampAuthority;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.eclipse.jetty.ee10.servlet.ServletTester;
import org.eclipse.jetty.http.HttpTester;
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

class TimeStampServletTest {
    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509Certificate createCert(KeyPair kp) throws Exception {
        X500Name name = new X500Name("CN=TSA" );
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
    @DisplayName("servlet returns timestamp reply")
    void testServlet() throws Exception {
        KeyPair kp = de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = createCert(kp);
        TimeStampAuthority auth = new TimeStampAuthority(kp.getPrivate(), cert, List.of(cert), "1.3.6.1.4.1.13762.3");
        TimeStampServlet servlet = new TimeStampServlet(auth);

        ServletTester tester = new ServletTester();
        tester.addServlet(servlet.getClass(), TimeStampServlet.PATH_MOUNT).setServlet(servlet);
        tester.start();
        try {
            byte[] data = MessageDigest.getInstance("SHA-512").digest("hi".getBytes());
            byte[] reqBytes = new TimeStampRequestGenerator().generate(TSPAlgorithms.SHA512, data).getEncoded();

            HttpTester.Request request = HttpTester.newRequest();
            request.setMethod("POST");
            request.setURI(TimeStampServlet.PATH_MOUNT);
            request.setVersion("HTTP/1.1");
            request.setHeader("Host", "tester");
            request.setHeader("Content-Type", "application/timestamp-query");
            request.setContent(reqBytes);

            HttpTester.Response response = HttpTester.parseResponse(tester.getResponses(request.generate()));
            assertEquals(200, response.getStatus());
            assertEquals("application/timestamp-reply", response.get("Content-Type"));
            TimeStampResponse tsResp = new TimeStampResponse(response.getContentBytes());
            assertEquals(TSPAlgorithms.SHA512, tsResp.getTimeStampToken().getTimeStampInfo().getHashAlgorithm().getAlgorithm());
            assertArrayEquals(data, tsResp.getTimeStampToken().getTimeStampInfo().getMessageImprintDigest());
        } finally {
            tester.stop();
        }
    }
}
