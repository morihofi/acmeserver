package de.morihofi.acmeserver.core.api.acme.api.endpoints;

import de.morihofi.acmeserver.core.api.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyChangeEndpointTest {

    static class DummyServerInstance implements IServerInstance {
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
        public RootCa getRootCa() { return null; }
        @Override
        public BuildMetadata getBuildMetadata() { return null; }
        @Override
        public INetworkClient getNetworkClient() { return null; }
    }

    @Test
    @DisplayName("Constructor creates instance")
    void testConstructor() {
        KeyChangeEndpoint endpoint = new KeyChangeEndpoint(new DummyServerInstance());
        assertNotNull(endpoint);
        assertInstanceOf(AbstractAcmeEndpoint.class, endpoint);
    }
}
