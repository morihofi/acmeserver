/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.tsa;

import de.morihofi.certgine.server.common.intf.ServletMount;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import de.morihofi.certgine.cryptography.tsa.TimeStampAuthority;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeStampServletTest {
    private static final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509Certificate createCert(KeyPair kp) throws Exception {
        X500Name name = new X500Name("CN=TSA" );
        Instant now = clock.instant();
        Instant tomorrow = now.plus(Duration.ofDays(1));
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                name, BigInteger.ONE, java.util.Date.from(now), java.util.Date.from(tomorrow),
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
        KeyPair kp = de.morihofi.certgine.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = createCert(kp);
        TimeStampAuthority auth = new TimeStampAuthority(kp.getPrivate(), cert, List.of(cert));
        TimeStampServlet servlet = new TimeStampServlet(auth);

        ServletTester tester = new ServletTester();
        ServletMount mount = TimeStampServlet.class.getAnnotation(ServletMount.class);
        tester.addServlet(servlet.getClass(), mount.servletMountPoint()).setServlet(servlet);
        tester.start();
        try {
            byte[] data = MessageDigest.getInstance("SHA-512").digest("hi".getBytes());
            byte[] reqBytes = new TimeStampRequestGenerator().generate(TSPAlgorithms.SHA512, data).getEncoded();

            HttpTester.Request request = HttpTester.newRequest();
            request.setMethod("POST");
            request.setURI(mount.servletMountPoint());
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

    @Test
    @DisplayName("non timestamp-query content type returns 415")
    void testUnsupportedContentType() throws Exception {
        TimeStampAuthority auth = new TimeStampAuthority(null, null, null) {
            @Override
            public byte[] generate(TimeStampRequest request) {
                return new byte[0];
            }
        };
        TimeStampServlet servlet = new TimeStampServlet(auth);

        ServletTester tester = new ServletTester();
        ServletMount mount = TimeStampServlet.class.getAnnotation(ServletMount.class);
        tester.addServlet(servlet.getClass(), mount.servletMountPoint()).setServlet(servlet);
        tester.start();
        try {
            HttpTester.Request request = HttpTester.newRequest();
            request.setMethod("POST");
            request.setURI(mount.servletMountPoint());
            request.setVersion("HTTP/1.1");
            request.setHeader("Host", "tester");
            request.setHeader("Content-Type", "text/plain");
            request.setContent("bad".getBytes());

            HttpTester.Response response = HttpTester.parseResponse(tester.getResponses(request.generate()));
            assertEquals(415, response.getStatus());
        } finally {
            tester.stop();
        }
    }

    @Test
    @DisplayName("corrupted request bytes return 400")
    void testCorruptedRequest() throws Exception {
        TimeStampAuthority auth = new TimeStampAuthority(null, null, null) {
            @Override
            public byte[] generate(TimeStampRequest request) {
                return new byte[0];
            }
        };
        TimeStampServlet servlet = new TimeStampServlet(auth);

        ServletTester tester = new ServletTester();
        ServletMount mount = TimeStampServlet.class.getAnnotation(ServletMount.class);
        tester.addServlet(servlet.getClass(), mount.servletMountPoint()).setServlet(servlet);
        tester.start();
        try {
            HttpTester.Request request = HttpTester.newRequest();
            request.setMethod("POST");
            request.setURI(mount.servletMountPoint());
            request.setVersion("HTTP/1.1");
            request.setHeader("Host", "tester");
            request.setHeader("Content-Type", "application/timestamp-query");
            request.setContent(new byte[]{0x00});

            HttpTester.Response response = HttpTester.parseResponse(tester.getResponses(request.generate()));
            assertEquals(400, response.getStatus());
        } finally {
            tester.stop();
        }
    }

    @Test
    @DisplayName("authority failure returns 500")
    void testAuthorityFailure() throws Exception {
        TimeStampAuthority auth = new TimeStampAuthority(null, null, null) {
            @Override
            public byte[] generate(TimeStampRequest request) {
                throw new RuntimeException("fail");
            }
        };
        TimeStampServlet servlet = new TimeStampServlet(auth);

        ServletTester tester = new ServletTester();
        ServletMount mount = TimeStampServlet.class.getAnnotation(ServletMount.class);
        tester.addServlet(servlet.getClass(), mount.servletMountPoint()).setServlet(servlet);
        tester.start();
        try {
            byte[] data = MessageDigest.getInstance("SHA-512").digest("hi".getBytes());
            byte[] reqBytes = new TimeStampRequestGenerator().generate(TSPAlgorithms.SHA512, data).getEncoded();

            HttpTester.Request request = HttpTester.newRequest();
            request.setMethod("POST");
            request.setURI(mount.servletMountPoint());
            request.setVersion("HTTP/1.1");
            request.setHeader("Host", "tester");
            request.setHeader("Content-Type", "application/timestamp-query");
            request.setContent(reqBytes);

            HttpTester.Response response = HttpTester.parseResponse(tester.getResponses(request.generate()));
            assertEquals(500, response.getStatus());
        } finally {
            tester.stop();
        }
    }
}
