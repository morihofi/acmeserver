/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order;

import de.morihofi.certgine.acme.AcmeModule;
import de.morihofi.certgine.acme.AcmeModuleInstance;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMEResourceNotFoundException;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import lombok.NonNull;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderInfoEndpointTest {

    private static AcmeModuleInstance moduleInstance() {
        DummyServerInstance server = new DummyServerInstance();
        return new AcmeModuleInstance(new AcmeModule(server));
    }

    @Test
    @DisplayName("Order expiration comes from stored order")
    void testOrderExpiration() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        OrderInfoEndpoint endpoint = new OrderInfoEndpoint(moduleInstance(), clock);
        AcmeOrder order = new AcmeOrder();
        Instant expires = now.plus(Duration.ofHours(1));
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
        OrderInfoEndpoint endpoint = new OrderInfoEndpoint(moduleInstance(), Clock.systemUTC());
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
        OrderInfoEndpoint endpoint = new OrderInfoEndpoint(moduleInstance(), Clock.systemUTC());
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

    static class DummyServerInstance implements IServerInstance {
        @Override
        public @NonNull String getServerURL() {
            return "";
        }

        @Override
        public @NonNull Session getDatabaseSession() {
            return null;
        }

        @Override
        public @NonNull de.morihofi.certgine.types.cryptography.ICryptoStoreManager getCryptoStoreManager() {
            return null;
        }

        @Override
        public @NonNull de.morihofi.certgine.types.config.Config getAppConfig() {
            return null;
        }

        @Override
        public @NonNull de.morihofi.certgine.types.database.entities.authority.RootCa getRootCa() {
            return null;
        }

        @Override
        public @NonNull de.morihofi.certgine.types.runtime.BuildMetadata getBuildMetadata() {
            return null;
        }

        @Override
        public @NonNull de.morihofi.certgine.types.intf.network.INetworkClient getNetworkClient() {
            return null;
        }

        @Override
        public @NonNull EventBus getEventBus() {
            return new EventBus();
        }

        @Override
        public @NonNull java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags() {
            return java.util.Collections.emptySet();
        }

        @Override
        public @NonNull IModuleRegistry getModuleRegistry() {
            return null;
        }
    }
}
