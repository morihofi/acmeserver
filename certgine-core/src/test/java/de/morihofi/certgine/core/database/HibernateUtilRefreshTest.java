/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.database;

import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.DatabaseConfig;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.events.ModuleEntityChangeEvent;
import jakarta.persistence.Entity;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link HibernateUtil#refreshSessionFactory(java.util.Collection)} rebuilds the factory.
 */
class HibernateUtilRefreshTest {

    @Test
    void refreshRebuildsSessionFactory() {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:refresh;DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);

        EventBus bus = new EventBus();
        ModuleRegistry registry = new ModuleRegistry(bus);
        registry.getEntityClasses().add(InitialEntity.class);

        HibernateUtil util = new HibernateUtil(cfg, true, bus, registry);
        SessionFactory first = util.getSessionFactory();
        assertNotNull(first);

        registry.getEntityClasses().add(NewEntity.class);
        bus.publish(new ModuleEntityChangeEvent(Set.copyOf(registry.getEntityClasses())));

        SessionFactory second = util.getSessionFactory();
        assertNotSame(first, second);
        assertTrue(first.isClosed());
    }

    @Entity
    static class InitialEntity {
        @jakarta.persistence.Id
        private Long id;
    }

    @Entity
    static class NewEntity {
        @jakarta.persistence.Id
        private Long id;
    }
}

