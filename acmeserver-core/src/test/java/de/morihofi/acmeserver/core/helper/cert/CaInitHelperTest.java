package de.morihofi.acmeserver.core.helper.cert;

import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.DatabaseConfig;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.events.EventBus;
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
        assertTrue(mgr.getKeyStore().containsAlias(first.getInternalUuid()));
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

        try (var s = hu.getSessionFactory().openSession()) {
            long count = s.createQuery("SELECT count(p) FROM AcmeProvisioner p", Long.class).uniqueResult();
            assertEquals(1, count);
        }

        String alias = mgr.getKeyStoreAliasForProvisionerIntermediate("default");
        assertTrue(mgr.getKeyStore().containsAlias(alias));
    }
}
