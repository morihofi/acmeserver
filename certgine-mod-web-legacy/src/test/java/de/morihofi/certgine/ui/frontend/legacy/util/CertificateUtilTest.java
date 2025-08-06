/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.ui.frontend.legacy.util;

import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.types.database.entities.authority.RsaCertificateAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CertificateUtilTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509Certificate generateCert() throws Exception {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName("Test")
                .countryCode("DE")
                .build();
        CertificateConfig cfg = new CertificateConfig(meta, new CertificateExpiration(0, 0, 1), new RsaCertificateAlgorithm(1024));
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        return X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg)
                .ownKeyPair(kp)
                .build());
    }

    @Test
    void testFingerprintSha1And256() throws Exception {
        X509Certificate cert = generateCert();
        String fpSha1 = CertificateUtil.getFingerprint(cert, "SHA-1");
        String fpSha256 = CertificateUtil.getFingerprint(cert, "SHA-256");

        MessageDigest md1 = MessageDigest.getInstance("SHA-1");
        MessageDigest md256 = MessageDigest.getInstance("SHA-256");
        String expected1 = de.morihofi.certgine.utils.conversion.HexConverter.bytesAsHexString(md1.digest(cert.getEncoded()));
        String expected256 = de.morihofi.certgine.utils.conversion.HexConverter.bytesAsHexString(md256.digest(cert.getEncoded()));

        assertEquals(expected1, fpSha1);
        assertEquals(expected256, fpSha256);
    }
}
