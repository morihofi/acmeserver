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

import static org.junit.jupiter.api.Assertions.assertThrows;

class X509GeneratorValidationTest {

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
    @DisplayName("intermediate generation fails when issuer path length is zero")
    void testIntermediatePathLenValidation() throws Exception {
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Root"))
                .ownKeyPair(rootKey)
                .build());

        KeyPair interKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate interCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.INTERMEDIATE_CA)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .ownKeyPair(interKey)
                .certificateConfig(cfg("Inter1"))
                .build());

        KeyPair nextKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        assertThrows(IllegalArgumentException.class, () ->
                X509Generator.generate(X509Generator.Request.builder()
                        .type(X509Generator.Type.INTERMEDIATE_CA)
                        .issuerKeyPair(interKey)
                        .issuerCertificate(interCert)
                        .ownKeyPair(nextKey)
                        .certificateConfig(cfg("Inter2"))
                        .build()));
    }

    @Test
    @DisplayName("server generation fails with non-CA issuer")
    void testServerIssuerMustBeCa() throws Exception {
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Root"))
                .ownKeyPair(rootKey)
                .build());

        KeyPair serverKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        DnsIdentifier id = new DnsIdentifier(DnsIdentifier.IDENTIFIER_TYPE.DNS, "example.com");
        Instant start = clock.instant();
        Instant end = start.plus(Duration.ofDays(1));

        X509Certificate serverCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.SERVER)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                .identifier(id)
                .startDate(java.util.Date.from(start))
                .endDate(java.util.Date.from(end))
                .build());

        assertThrows(IllegalArgumentException.class, () ->
                X509Generator.generate(X509Generator.Request.builder()
                        .type(X509Generator.Type.SERVER)
                        .issuerKeyPair(serverKey)
                        .issuerCertificate(serverCert)
                        .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                        .identifier(id)
                        .startDate(java.util.Date.from(start))
                        .endDate(java.util.Date.from(end))
                        .build()));
    }
}
