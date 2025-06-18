package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.server.StartupFlag;
import lombok.NonNull;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IServerInstanceTest {

    static class DummyInstance implements IServerInstance {
        private final RootCa rootCa;

        DummyInstance(String alias) {
            this.rootCa = new RootCa();
            this.rootCa.setInternalUuid(alias);
        }

        @NotNull
        @Override
        public String getServerURL() { return ""; }

        @NotNull
        @Override
        public Session getDatabaseSession() { return null; }

        @NotNull
        @Override
        public ICryptoStoreManager getCryptoStoreManager() { return null; }

        @NotNull
        @Override
        public Config getAppConfig() { return null; }

        @NotNull
        @Override
        public INonceManager getNonceManager() { return null; }

        @NotNull
        @Override
        public RootCa getRootCa() { return rootCa; }

        @NotNull
        @Override
        public BuildMetadata getBuildMetadata() { return null; }

        @NotNull
        @Override
        public INetworkClient getNetworkClient() { return null; }

        @NotNull
        @Override
        public EventBus getEventBus() { return null; }

        @Override
        public @NonNull Set<StartupFlag> getStartupFlags() {
            return Set.of();
        }
    }

    @Test
    @DisplayName("getRootCaAlias returns internal UUID")
    void testGetRootCaAlias() {
        DummyInstance di = new DummyInstance("abc");
        assertEquals("abc", di.getRootCaAlias());
    }
}

