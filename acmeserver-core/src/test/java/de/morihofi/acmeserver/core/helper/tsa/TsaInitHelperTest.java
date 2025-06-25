package de.morihofi.acmeserver.core.helper.tsa;

import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.core.helper.cert.CaInitHelper;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.DatabaseConfig;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.database.entities.authority.RootCa;
import de.morihofi.acmeserver.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.acmeserver.types.events.EventBus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class TsaInitHelperTest {
    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("initializeTsa persists TSA")
    void testInitialize() throws Exception {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:tsa;DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);

        EventBus bus = new EventBus();
        HibernateUtil hu = new HibernateUtil(cfg, true, bus);

        Path ks = Files.createTempDirectory("ks").resolve("store.p12");
        CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(ks, "pw".toCharArray()));

        RootCa root = CaInitHelper.initializeCA(hu, mgr, bus);
        TsaAuthority first = TsaInitHelper.initializeTsa(hu, mgr, root, bus);
        TsaAuthority second = TsaInitHelper.initializeTsa(hu, mgr, root, bus);

        String alias = mgr.getKeyStoreAliasForTimestampAuthority(first.getInternalUuid());
        assertEquals(first.getInternalUuid(), second.getInternalUuid());
        assertTrue(mgr.containsTimestampAuthorityCertificate(first.getInternalUuid()));
    }
}
