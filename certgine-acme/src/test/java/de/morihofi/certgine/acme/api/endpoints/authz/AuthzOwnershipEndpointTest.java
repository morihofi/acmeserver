/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints.authz;
import de.morihofi.certgine.types.database.entities.authority.RootCa;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.events.EventBus;

import de.morihofi.certgine.types.database.entities.acme.AcmeOrder;
import de.morihofi.certgine.types.database.entities.acme.AcmeOrderIdentifier;
import de.morihofi.certgine.types.intf.IServerInstance;
import org.hibernate.Session;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthzOwnershipEndpointTest {

    static class DummyServerInstance implements IServerInstance {
        @NotNull
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @NonNull
        @Override public Session getDatabaseSession() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.config.Config getAppConfig() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.intf.INonceManager getNonceManager() { return null; }
        @NotNull
        @NonNull
        @Override public RootCa getRootCa() { return null; }
        @NotNull
        @NonNull
        @Override public TsaAuthority getTsaAuthority() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NotNull
        @NonNull
        @Override public de.morihofi.certgine.types.intf.network.INetworkClient getNetworkClient() { return null; }
        @NotNull
        @NonNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @NotNull
        @NonNull
        @Override public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
    }


    @Test
    @DisplayName("Authorization expiration comes from associated order")
    void testAuthorizationExpiration() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AuthzOwnershipEndpoint endpoint = new AuthzOwnershipEndpoint(new DummyServerInstance(), clock);
        AcmeOrder order = new AcmeOrder();
        Instant expires = now.plus(Duration.ofHours(1));
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
