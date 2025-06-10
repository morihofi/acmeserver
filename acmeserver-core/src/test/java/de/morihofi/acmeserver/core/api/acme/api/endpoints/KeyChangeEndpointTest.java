package de.morihofi.acmeserver.core.api.acme.api.endpoints;

import de.morihofi.acmeserver.types.intf.IServerInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyChangeEndpointTest {

    static class DummyServerInstance implements IServerInstance {
        @Override
        public String getServerURL() { return ""; }
        @Override
        public org.hibernate.Session getDatabaseSession() { return null; }
        @Override
        public de.morihofi.acmeserver.types.intf.crypto.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @Override
        public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @Override
        public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @Override
        public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @Override
        public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @Override
        public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
    }

    @Test
    @DisplayName("Constructor creates instance")
    void testConstructor() {
        KeyChangeEndpoint endpoint = new KeyChangeEndpoint(new DummyServerInstance());
        assertNotNull(endpoint);
        assertTrue(endpoint instanceof de.morihofi.acmeserver.core.api.acme.api.abstractclass.AbstractAcmeEndpoint);
    }
}
