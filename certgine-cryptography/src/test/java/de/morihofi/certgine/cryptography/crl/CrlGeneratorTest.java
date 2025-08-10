/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.crl;

import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.cryptography.revoke.RevocationReason;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class CrlGeneratorTest {

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
    @DisplayName("generate creates CRL with entries")
    void testGenerate() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate caCert = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.ROOT_CA)
                        .certificateConfig(cfg("Test CA"))
                        .ownKeyPair(kp)
                        .build());

        RevokedCertificate rc = new RevokedCertificate(BigInteger.ONE, clock.instant(),
                RevocationReason.UNSPECIFIED);

        X509CRL crl = CrlGenerator.generate(
                CrlGenerator.Request.builder()
                        .revokedCertificate(rc)
                        .caCert(caCert)
                        .caPrivateKey(kp.getPrivate())
                        .updateMinutes(5)
                        .build());

        assertNotNull(crl);
        assertNotNull(crl.getRevokedCertificate(BigInteger.ONE));
    }

    @Test
    @DisplayName("generated CRL timestamps use UTC")
    void testCrlTimestampsUtc() throws Exception {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
        try {
            KeyPair kp = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
            X509Certificate caCert = X509Generator.generate(
                    X509Generator.Request.builder()
                            .type(X509Generator.Type.ROOT_CA)
                            .certificateConfig(cfg("Test CA UTC"))
                            .ownKeyPair(kp)
                            .build());
            X509CRL crl = CrlGenerator.generate(
                    CrlGenerator.Request.builder()
                            .caCert(caCert)
                            .caPrivateKey(kp.getPrivate())
                            .updateMinutes(10)
                            .build());
            Instant thisUpdate = crl.getThisUpdate().toInstant();
            Instant nextUpdate = crl.getNextUpdate().toInstant();
            assertEquals(Duration.ofMinutes(10), Duration.between(thisUpdate, nextUpdate));
            String thisStr = DateTimeFormatter.ISO_INSTANT.format(thisUpdate);
            String nextStr = DateTimeFormatter.ISO_INSTANT.format(nextUpdate);
            assertTrue(thisStr.endsWith("Z"));
            assertTrue(nextStr.endsWith("Z"));
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
