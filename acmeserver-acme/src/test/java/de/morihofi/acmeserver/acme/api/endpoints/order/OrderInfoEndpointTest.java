package de.morihofi.acmeserver.acme.api.endpoints.order;
import de.morihofi.acmeserver.types.database.entities.authority.RootCa;
import de.morihofi.acmeserver.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.acmeserver.types.events.EventBus;

import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrder;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEResourceNotFoundException;
import org.hibernate.Session;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderInfoEndpointTest {

    static class DummyServerInstance implements IServerInstance {
        @NotNull
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @NonNull
        @Override public Session getDatabaseSession() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @NotNull
        @NonNull
        @Override public RootCa getRootCa() { return null; }
        @NotNull
        @NonNull
        @Override public TsaAuthority getTsaAuthority() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
            @NotNull
            @NonNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @NotNull
        @NonNull
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
    }

    @Test
    @DisplayName("Order expiration comes from stored order")
    void testOrderExpiration() {
        OrderInfoEndpoint endpoint = new OrderInfoEndpoint(new DummyServerInstance());
        AcmeOrder order = new AcmeOrder();
        Timestamp expires = Timestamp.from(Instant.now().plusSeconds(3600));
        order.setExpires(expires);

        java.lang.reflect.Method m;
        try {
            m = OrderInfoEndpoint.class.getDeclaredMethod("getOrderExpiration", AcmeOrder.class);
            m.setAccessible(true);
            assertEquals(expires, m.invoke(endpoint, order));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("verifyIdentifiersPresent throws when list empty")
    void testVerifyIdentifiersPresentThrows() {
        OrderInfoEndpoint endpoint = new OrderInfoEndpoint(new DummyServerInstance());
        java.lang.reflect.Method m;
        try {
            m = OrderInfoEndpoint.class.getDeclaredMethod("verifyIdentifiersPresent", String.class, List.class);
            m.setAccessible(true);
            java.lang.reflect.InvocationTargetException ex = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> m.invoke(endpoint, "test", List.of()));
            assertTrue(ex.getCause() instanceof ACMEResourceNotFoundException);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("verifyIdentifiersPresent passes with identifiers")
    void testVerifyIdentifiersPresentOk() {
        OrderInfoEndpoint endpoint = new OrderInfoEndpoint(new DummyServerInstance());
        AcmeOrderIdentifier id = new AcmeOrderIdentifier("dns", "example.com");
        java.lang.reflect.Method m;
        try {
            m = OrderInfoEndpoint.class.getDeclaredMethod("verifyIdentifiersPresent", String.class, List.class);
            m.setAccessible(true);
            assertDoesNotThrow(() -> m.invoke(endpoint, "test", List.of(id)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
