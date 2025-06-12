package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.events.EventBus;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IServerInstanceTest {

    static class DummyInstance implements IServerInstance {
        private final RootCa rootCa;

        DummyInstance(String alias) {
            this.rootCa = new RootCa();
            this.rootCa.setInternalUuid(alias);
        }

        @Override
        public String getServerURL() { return ""; }

        @Override
        public Session getDatabaseSession() { return null; }

        @Override
        public ICryptoStoreManager getCryptoStoreManager() { return null; }

        @Override
        public Config getAppConfig() { return null; }

        @Override
        public INonceManager getNonceManager() { return null; }

        @Override
        public RootCa getRootCa() { return rootCa; }

        @Override
        public BuildMetadata getBuildMetadata() { return null; }

        @Override
        public INetworkClient getNetworkClient() { return null; }

        @Override
        public EventBus getEventBus() { return null; }
    }

    @Test
    @DisplayName("getRootCaAlias returns internal UUID")
    void testGetRootCaAlias() {
        DummyInstance di = new DummyInstance("abc");
        assertEquals("abc", di.getRootCaAlias());
    }
}

