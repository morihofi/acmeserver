package de.morihofi.acmeserver.core.api.acme.api.endpoints.authz;

import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuthzOwnershipEndpointTest {

    static class DummyServerInstance implements IServerInstance {
        @Override public String getServerURL() { return ""; }
        @Override public Session getDatabaseSession() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
    }

    @Test
    @DisplayName("Authorization expiration comes from associated order")
    void testAuthorizationExpiration() {
        AuthzOwnershipEndpoint endpoint = new AuthzOwnershipEndpoint(new DummyServerInstance());
        AcmeOrder order = new AcmeOrder();
        Timestamp expires = Timestamp.from(Instant.now().plusSeconds(3600));
        order.setExpires(expires);
        AcmeOrderIdentifier identifier = new AcmeOrderIdentifier("dns", "example.com");
        identifier.setOrder(order);

        assertEquals(expires, endpoint.getAuthorizationExpiration(identifier));
    }
}
