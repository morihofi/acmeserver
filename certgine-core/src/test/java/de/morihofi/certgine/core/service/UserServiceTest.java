package de.morihofi.certgine.core.service;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.DatabaseConfig;
import de.morihofi.certgine.types.database.entities.user.Users;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.server.StartupFlag;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    static class DummySI implements IServerInstance {
        private final HibernateUtil hu;
        private final Config cfg;
        DummySI(HibernateUtil hu, Config cfg){this.hu=hu;this.cfg=cfg;}
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
        @Override public de.morihofi.certgine.types.database.entities.authority.RootCa getRootCa(){return null;}
        @NotNull
        @Override public de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority getTsaAuthority(){return null;}
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
    void loginAndSession() throws Exception {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:users;DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);

        EventBus bus = new EventBus();
        HibernateUtil hu = new HibernateUtil(cfg, true, bus);

        IServerInstance si = new DummySI(hu, cfg);
        UserService us = new UserService(si);

        Users u = us.register("test@example.com", "pw");
        UserService.LoginResult lr = us.login("test@example.com", "pw", null, null);
        assertNotNull(lr);
        assertNotNull(lr.getToken());
        Users fromToken = us.getUserFromToken(lr.getToken());
        assertEquals(u.getId(), fromToken.getId());
        assertEquals(1, us.getSessionsForUser(u).size());
    }
}
