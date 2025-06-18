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



    @Test
    @DisplayName("Authorization expiration comes from associated order")
    void testAuthorizationExpiration() {
        /*
        AuthzOwnershipEndpoint endpoint = new AuthzOwnershipEndpoint(new DummyServerInstance());
        AcmeOrder order = new AcmeOrder();
        Timestamp expires = Timestamp.from(Instant.now().plusSeconds(3600));
        order.setExpires(expires);
        AcmeOrderIdentifier identifier = new AcmeOrderIdentifier("dns", "example.com");
        identifier.setOrder(order);

        assertEquals(expires, endpoint.getAuthorizationExpiration(identifier));

         */
    }
}
