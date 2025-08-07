/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.keystore;

import com.google.common.jimfs.Jimfs;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class CryptoStoreManagerTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("constructor creates PKCS12 keystore")
    void testConstructor() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("test.p12");
            PKCS12KeyStoreConfig cfg = new PKCS12KeyStoreConfig(path, "pw".toCharArray());
            CryptoStoreManager mgr = new CryptoStoreManager(cfg, new Pkcs12KeyStoreLoader(cfg));
            var field = CryptoStoreManager.class.getDeclaredField("keyStore");
            field.setAccessible(true);
            assertNotNull(field.get(mgr));
            mgr.saveKeystore();
            assertTrue(Files.exists(path));
        }
    }

    @Test
    @DisplayName("containsAlias reflects stored entries")
    void testContainsAlias() throws Exception {
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path path = fs.getPath("test2.p12");
            PKCS12KeyStoreConfig cfg = new PKCS12KeyStoreConfig(path, "pw".toCharArray());
            CryptoStoreManager mgr = new CryptoStoreManager(cfg, new Pkcs12KeyStoreLoader(cfg));
            RootCa rc = new RootCa();
            rc.setInternalUuid("root");

            assertFalse(mgr.containsCertificateAuthority(rc));

            KeyPair kp = de.morihofi.certgine.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
            CertificateConfig conf = new CertificateConfig(
                    CertificateMetadata.builder().commonName("root").build(),
                    new CertificateExpiration(0, 0, 1),
                    null);
            rc.setCertificateConfig(conf);
            X509Certificate cert = de.morihofi.certgine.cryptography.certificate.X509Generator.generate(
                    de.morihofi.certgine.cryptography.certificate.X509Generator.Request.builder()
                            .type(de.morihofi.certgine.cryptography.certificate.X509Generator.Type.ROOT_CA)
                            .certificateConfig(conf)
                            .ownKeyPair(kp)
                            .build());
            mgr.addCertificateAuthority(rc, kp, cert);

            assertTrue(mgr.containsCertificateAuthority(rc));
            assertNotNull(mgr.getCertificate(rc.getInternalUuid()));
        }
    }
}
