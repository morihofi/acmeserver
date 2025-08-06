package de.morihofi.certgine.core.impl;

import de.morihofi.certgine.acme.security.NonceManager;
import de.morihofi.certgine.acme.types.entities.AcmeHttpNonce;
import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.DatabaseConfig;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadNonceException;
import de.morihofi.certgine.types.intf.IServerInstance;
import jakarta.persistence.Entity;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NonceManagerTest {

    private HibernateUtil createHibernateUtil(String dbName) {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);
        Set<Class<?>> entities = new HashSet<>(
                new Reflections("de.morihofi.certgine.types.database")
                        .getTypesAnnotatedWith(Entity.class));
        entities.add(AcmeHttpNonce.class);
        return new HibernateUtil(cfg, true, new EventBus(), entities);
    }

    @Test
    @DisplayName("known nonce accepted once and rejected on reuse")
    void testKnownNonceReuse() {
        var hu = createHibernateUtil("known");
        String nonce = "knownNonce";
        try (Session s = hu.getSessionFactory().openSession()) {
            var tx = s.beginTransaction();
            s.persist(new AcmeHttpNonce(nonce));
            tx.commit();
        }
        IServerInstance si = Mockito.mock(IServerInstance.class);
        Mockito.when(si.getDatabaseSession()).thenAnswer(inv -> hu.getSessionFactory().openSession());
        Mockito.when(si.getEventBus()).thenReturn(new EventBus());
        NonceManager manager = new NonceManager(si);
        assertFalse(manager.isNonceUsed(nonce));
        assertTrue(manager.isNonceUsed(nonce));
    }

    @Test
    @DisplayName("debug mode bypasses nonce checks")
    void testDebugModeBypassesChecks() {
        var hu = createHibernateUtil("debug");
        IServerInstance si = Mockito.mock(IServerInstance.class);
        Mockito.when(si.getDatabaseSession()).thenAnswer(inv -> hu.getSessionFactory().openSession());
        Mockito.when(si.getEventBus()).thenReturn(new EventBus());
        NonceManager manager = new NonceManager(si, true);
        assertFalse(manager.isNonceUsed("any"));
        assertDoesNotThrow(() -> manager.checkNonceFromDecodedProtected("{\"nonce\":\"any\"}"));
    }

    @Test
    @DisplayName("unknown nonce throws exception")
    void testUnknownNonceThrows() {
        var hu = createHibernateUtil("unknown");
        IServerInstance si = Mockito.mock(IServerInstance.class);
        Mockito.when(si.getDatabaseSession()).thenAnswer(inv -> hu.getSessionFactory().openSession());
        Mockito.when(si.getEventBus()).thenReturn(new EventBus());
        NonceManager manager = new NonceManager(si);
        assertThrows(ACMEBadNonceException.class,
                () -> manager.checkNonceFromDecodedProtected("{\"nonce\":\"nope\"}"));
    }
}

