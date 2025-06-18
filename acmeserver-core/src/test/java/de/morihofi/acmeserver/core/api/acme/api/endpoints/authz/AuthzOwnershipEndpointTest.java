package de.morihofi.acmeserver.core.api.acme.api.endpoints.authz;
import de.morihofi.acmeserver.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.types.events.EventBus;

import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthzOwnershipEndpointTest {

    static class DummyServerInstance implements IServerInstance {
        @NotNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @Override public Session getDatabaseSession() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
        @NotNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @NotNull
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
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

        java.lang.reflect.Method m;
        try {
            m = AuthzOwnershipEndpoint.class.getDeclaredMethod("getAuthorizationExpiration", AcmeOrderIdentifier.class);
            m.setAccessible(true);
            assertEquals(expires, m.invoke(endpoint, identifier));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
