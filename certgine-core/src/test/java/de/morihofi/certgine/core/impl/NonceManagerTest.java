package de.morihofi.certgine.core.impl;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.DatabaseConfig;
import de.morihofi.certgine.types.database.entities.acme.HttpNonces;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadNonceException;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NonceManagerTest {

    private HibernateUtil createHibernateUtil(String dbName) {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);
        return new HibernateUtil(cfg, true, new EventBus());
    }

    @Test
    @DisplayName("known nonce accepted once and rejected on reuse")
    void testKnownNonceReuse() {
        var hu = createHibernateUtil("known");
        String nonce = "knownNonce";
        try (Session s = hu.getSessionFactory().openSession()) {
            var tx = s.beginTransaction();
            s.persist(new HttpNonces(nonce));
            tx.commit();
        }
        NonceManager manager = new NonceManager(hu, new EventBus());
        assertFalse(manager.isNonceUsed(nonce));
        assertTrue(manager.isNonceUsed(nonce));
    }

    @Test
    @DisplayName("debug mode bypasses nonce checks")
    void testDebugModeBypassesChecks() {
        var hu = createHibernateUtil("debug");
        NonceManager manager = new NonceManager(hu, new EventBus(), true);
        assertFalse(manager.isNonceUsed("any"));
        assertDoesNotThrow(() -> manager.checkNonceFromDecodedProtected("{\"nonce\":\"any\"}"));
    }

    @Test
    @DisplayName("unknown nonce throws exception")
    void testUnknownNonceThrows() {
        var hu = createHibernateUtil("unknown");
        NonceManager manager = new NonceManager(hu, new EventBus());
        assertThrows(ACMEBadNonceException.class,
                () -> manager.checkNonceFromDecodedProtected("{\"nonce\":\"nope\"}"));
    }
}

