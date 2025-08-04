/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.certificate;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class X509CertificateToolsTest {

    private static final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509Certificate createCert() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(1024);
        KeyPair kp = kpg.generateKeyPair();
        X500Name name = new X500Name("CN=test");
        Instant now = clock.instant();
        Instant later = now.plusSeconds(10);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE, java.util.Date.from(now), java.util.Date.from(later), name, kp.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);
    }

    @Test
    @DisplayName("certificateToPEM converts and back")
    void testCertificateToPem() throws Exception {
        X509Certificate cert = createCert();
        String pem = X509CertificateTools.certificateToPEM(cert.getEncoded());
        X509Certificate parsed = X509CertificateTools.convertToX509Cert(PemUtil.convertPemToByteArray(pem));
        assertEquals(cert.getSubjectX500Principal(), parsed.getSubjectX500Principal());
    }

    @Test
    @DisplayName("isCertificateCurrentlyDateValid works")
    void testDateValid() throws Exception {
        X509Certificate cert = createCert();
        assertTrue(X509CertificateTools.isCertificateCurrentlyDateValid(cert, clock));
    }
}
