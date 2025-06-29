/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.entities;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.DatabaseConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.authority.RsaCertificateAlgorithm;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.server.StartupFlag;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RootCaGetForUuidTest {
    static class DummySI implements IServerInstance {
        private final HibernateUtil hu;
        private final RootCa ca;
        private final Config cfg;
        DummySI(HibernateUtil hu, RootCa ca, Config cfg){this.hu=hu;this.ca=ca;this.cfg=cfg;}
        @NotNull
        @Override public String getServerURL(){return "";}
        @NotNull
        @Override public Session getDatabaseSession(){return hu.getSessionFactory().openSession();}
        @NotNull
        @Override public ICryptoStoreManager getCryptoStoreManager(){return null;}
        @NotNull
        @Override public Config getAppConfig(){return cfg;}
        @NotNull
        @Override public INonceManager getNonceManager(){return null;}
        @NotNull
        @Override public RootCa getRootCa(){return ca;}
        @NotNull
        @Override public TsaAuthority getTsaAuthority(){return null;}
        @NotNull
        @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @NotNull
        @Override public INetworkClient getNetworkClient(){return null;}
        @NotNull
        @Override public EventBus getEventBus(){return new EventBus();}
        @NotNull
        @Override public Set<StartupFlag> getStartupFlags(){return Collections.emptySet();}
    }

    @Test
    @DisplayName("getForUuid returns CA by UUID")
    void testGetForUuid() throws Exception {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:uuid;DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);

        EventBus bus = new EventBus();
        HibernateUtil hu = new HibernateUtil(cfg, true, bus);

        RootCa ca = new RootCa();
        ca.setInternalUuid("abc");
        ca.setCertificateConfig(new CertificateConfig(
                CertificateMetadata.builder()
                        .commonName("root")
                        .build(),
                null,
                new RsaCertificateAlgorithm(1024)));

        try (Session s = hu.getSessionFactory().openSession()) {
            var tx = s.beginTransaction();
            s.persist(ca);
            tx.commit();
        }

        IServerInstance si = new DummySI(hu, ca, cfg);
        RootCa loaded = RootCa.getForUuid(si, "abc");
        assertNotNull(loaded);
        assertEquals("abc", loaded.getInternalUuid());
    }
}
