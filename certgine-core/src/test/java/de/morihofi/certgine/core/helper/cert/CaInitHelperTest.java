/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.helper.cert;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.DatabaseConfig;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.events.EventBus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class CaInitHelperTest {
    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("initializeCA persists root CA")
    void testInitializeCAPersists() throws Exception {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);

        EventBus bus = new EventBus();
        HibernateUtil hu = new HibernateUtil(cfg, true, bus);

        Path ks = Files.createTempDirectory("ks").resolve("store.p12");
        CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(ks, "pw".toCharArray()));

        RootCa first = CaInitHelper.initializeCA(hu, mgr, bus);
        RootCa second = CaInitHelper.initializeCA(hu, mgr, bus);

        assertEquals(first.getInternalUuid(), second.getInternalUuid());
        assertTrue(mgr.containsCertificateAuthority(first));
    }

    @Test
    @DisplayName("initializeCA creates default provisioner")
    void testDefaultProvisioner() throws Exception {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:testdb2;DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);

        EventBus bus = new EventBus();
        HibernateUtil hu = new HibernateUtil(cfg, true, bus);

        Path ks = Files.createTempDirectory("ks").resolve("store2.p12");
        CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(ks, "pw".toCharArray()));

        RootCa root = CaInitHelper.initializeCA(hu, mgr, bus);

        AcmeProvisioner provisioner;
        try (var s = hu.getSessionFactory().openSession()) {
            long count = s.createQuery("SELECT count(p) FROM AcmeProvisioner p", Long.class).uniqueResult();
            assertEquals(1, count);
            provisioner = s.createQuery("FROM AcmeProvisioner", AcmeProvisioner.class).getSingleResult();
        }

        assertTrue(mgr.containsIntermediateCaCertificate(provisioner.getInternalUuid()));
    }
}
