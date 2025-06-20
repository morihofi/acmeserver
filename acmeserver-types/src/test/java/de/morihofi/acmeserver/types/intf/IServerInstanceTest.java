package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.server.StartupFlag;
import lombok.NonNull;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IServerInstanceTest {

    static class DummyInstance implements IServerInstance {
        private final RootCa rootCa;

        DummyInstance(String alias) {
            this.rootCa = new RootCa();
            this.rootCa.setInternalUuid(alias);
        }

        @NonNull
        @Override
        public String getServerURL() { return ""; }

        @NonNull
        @Override
        public Session getDatabaseSession() { return null; }

        @NonNull
        @Override
        public ICryptoStoreManager getCryptoStoreManager() { return null; }

        @NonNull
        @Override
        public Config getAppConfig() { return null; }

        @NonNull
        @Override
        public INonceManager getNonceManager() { return null; }

        @NonNull
        @Override
        public RootCa getRootCa() { return rootCa; }

        @NonNull
        @Override
        public BuildMetadata getBuildMetadata() { return null; }

        @NonNull
        @Override
        public INetworkClient getNetworkClient() { return null; }

        @NonNull
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

