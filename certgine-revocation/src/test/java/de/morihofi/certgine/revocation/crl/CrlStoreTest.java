/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.crl;

import de.morihofi.certgine.cryptography.crl.CrlGenerator;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.LocalTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CrlStoreTest {

    @BeforeAll
    static void setup(){
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
    @DisplayName("getCertificateStatus detects revoked certificate")
    void testGetStatus() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate caCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Test"))
                .ownKeyPair(kp)
                .build());
        RevokedCertificate rc = new RevokedCertificate(BigInteger.ONE, new Date(), 0);
        X509CRL crl = CrlGenerator.generate(CrlGenerator.Request.builder()
                .revokedCertificate(rc)
                .caCert(caCert)
                .caPrivateKey(kp.getPrivate())
                .updateMinutes(5)
                .build());
        CrlStore.entryMap.put("p", new CrlStore.CrlEntry(LocalTime.now(), crl));

        CertificateStatus status = CrlStore.getCertificateStatus(BigInteger.ONE, "p");
        assertTrue(status instanceof RevokedStatus);
        CertificateStatus good = CrlStore.getCertificateStatus(BigInteger.TEN, "p");
        assertEquals(CertificateStatus.GOOD, good);
    }

    @Test
    @DisplayName("getCrlForProvisioner throws when missing")
    void testMissing() {
        CrlStore.entryMap.clear();
        assertThrows(IllegalArgumentException.class, () -> CrlStore.getCrlForProvisioner("missing"));
    }
}
