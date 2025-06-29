/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.certificate;

import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeSigningCertificateTest {

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
    @DisplayName("code signing certificate has correct attributes")
    void testCodeSigning() throws Exception {
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Root"))
                .ownKeyPair(rootKey)
                .build());

        KeyPair csKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate csCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.CODE_SIGNING)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .ownKeyPair(csKey)
                .certificateConfig(cfg("Codesigner"))
                .build());

        csCert.verify(rootKey.getPublic());
        assertEquals(-1, csCert.getBasicConstraints());
        boolean[] ku = csCert.getKeyUsage();
        assertTrue(ku[0]);
        assertTrue(ku[1]);
        List<String> eku = csCert.getExtendedKeyUsage();
        assertEquals(1, eku.size());
        assertEquals(KeyPurposeId.id_kp_codeSigning.getId(), eku.getFirst());
    }
}
