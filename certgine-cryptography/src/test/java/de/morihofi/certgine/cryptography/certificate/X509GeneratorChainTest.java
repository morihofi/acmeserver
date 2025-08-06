/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.certificate;

import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.types.dns.DnsIdentifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class X509GeneratorChainTest {

    private static final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName(cn)
                .organisation("Org")
                .countryCode("DE")
                .build();
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }

    @Test
    @DisplayName("root, intermediate and server certificates form valid chain")
    void testFullChain() throws Exception {
        // --- Root CA ---
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Test Root"))
                .ownKeyPair(rootKey)
                .build());

        rootCert.verify(rootKey.getPublic());
        assertTrue(rootCert.getBasicConstraints() > 0);
        boolean[] ku = rootCert.getKeyUsage();
        assertTrue(ku[5] && ku[6]);

        // --- Intermediate CA ---
        KeyPair interKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate interCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.INTERMEDIATE_CA)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .ownKeyPair(interKey)
                .certificateConfig(cfg("Test Intermediate"))
                .crlDistributionUrl("http://example.com/crl")
                .ocspServiceEndpoint("http://example.com/ocsp")
                .build());

        interCert.verify(rootKey.getPublic());
        assertEquals(0, interCert.getBasicConstraints());
        ku = interCert.getKeyUsage();
        assertTrue(ku[5] && ku[6]);

        // --- Server certificate ---
        KeyPair serverKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        DnsIdentifier id = new DnsIdentifier(DnsIdentifier.IDENTIFIER_TYPE.DNS, "example.com");
        Instant start = clock.instant();
        Instant end = start.plus(Duration.ofDays(1));

        X509Certificate serverCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.SERVER)
                .issuerKeyPair(interKey)
                .issuerCertificate(interCert)
                .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                .identifier(id)
                .startDate(java.util.Date.from(start))
                .endDate(java.util.Date.from(end))
                .build());

        serverCert.verify(interKey.getPublic());
        assertEquals(-1, serverCert.getBasicConstraints());
        ku = serverCert.getKeyUsage();
        assertTrue(ku[0] && ku[2]);
        assertTrue(serverCert.getSubjectAlternativeNames()
                .stream()
                .anyMatch(l -> "example.com".equals(l.get(1))));
    }
}
