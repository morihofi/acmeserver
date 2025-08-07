/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.authz;

import de.morihofi.certgine.acme.AcmeModule;
import de.morihofi.certgine.acme.AcmeModuleInstance;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier;
import de.morihofi.certgine.core.util.DummyServerInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthzOwnershipEndpointTest {

    @Test
    @DisplayName("Authorization expiration comes from associated order")
    void testAuthorizationExpiration() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AuthzOwnershipEndpoint endpoint = new AuthzOwnershipEndpoint(new AcmeModuleInstance(new AcmeModule(new DummyServerInstance())), clock);
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
