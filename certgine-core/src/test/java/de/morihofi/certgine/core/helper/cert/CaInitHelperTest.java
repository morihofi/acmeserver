/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.helper.cert;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.cryptography.keystore.Pkcs12KeyStoreLoader;
import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.DatabaseConfig;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.events.EventBus;
import jakarta.persistence.Entity;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Set<Class<?>> entities = new HashSet<>(
                new Reflections("de.morihofi.certgine.types.database")
                        .getTypesAnnotatedWith(Entity.class));
        ModuleRegistry registry = new ModuleRegistry(bus);
        registry.getEntityClasses().addAll(entities);
        HibernateUtil hu = new HibernateUtil(cfg, true, bus, registry);

        Path ks = Files.createTempDirectory("ks").resolve("store.p12");
        PKCS12KeyStoreConfig ksCfg = new PKCS12KeyStoreConfig(ks, "pw".toCharArray());
        CryptoStoreManager mgr = new CryptoStoreManager(ksCfg, new Pkcs12KeyStoreLoader(ksCfg));

        RootCa first = CaInitHelper.initializeCA(hu, mgr, bus);
        RootCa second = CaInitHelper.initializeCA(hu, mgr, bus);

        assertEquals(first.getInternalUuid(), second.getInternalUuid());
        assertTrue(mgr.containsCertificateAuthority(first));
    }

}
