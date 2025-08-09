package de.morihofi.certgine.acme.servlets.handlerapi.endpoints;

import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.NewOrderEndpoint.IdentifierProcessingResult;
import de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier;
import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.events.NewAcmeOrderEvent;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NewOrderEndpointTest {

    @Test
    void processIdentifiersReturnsAuthorizationUrls() throws Exception {
        CertgineModuleInstance moduleInstance = mock(CertgineModuleInstance.class);
        NewOrderEndpoint endpoint = new NewOrderEndpoint(moduleInstance, Clock.systemUTC());
        AcmeProvisioner provisioner = mock(AcmeProvisioner.class);
        IServerInstance serverInstance = mock(IServerInstance.class);
        when(provisioner.isWildcardAllowed()).thenReturn(true);
        when(provisioner.isIpAllowed()).thenReturn(false);
        when(provisioner.getAcmeApiURL(serverInstance)).thenReturn("https://acme.test");
        var restriction = new de.morihofi.certgine.acme.types.entities.AcmeProvisionerDomainNameRestriction();
        restriction.setEnabled(false);
        when(provisioner.getAcmeProvisionerDomainNameRestriction()).thenReturn(restriction);

        List<AcmeOrderIdentifier> request = List.of(new AcmeOrderIdentifier("dns", "example.com"));

        Method m = NewOrderEndpoint.class.getDeclaredMethod("processIdentifiers", List.class, AcmeProvisioner.class, IServerInstance.class);
        m.setAccessible(true);
        IdentifierProcessingResult result = (IdentifierProcessingResult) m.invoke(endpoint, request, provisioner, serverInstance);

        assertEquals(1, result.getEntityIdentifiers().size());
        assertEquals("example.com", result.getEntityIdentifiers().get(0).getDataValue());
        assertEquals(1, result.getAuthorizations().size());
        assertTrue(result.getAuthorizations().get(0).contains("/acme/authz/"));
    }

    @Test
    void persistOrderPersistsAndPublishesEvent() throws Exception {
        CertgineModuleInstance moduleInstance = mock(CertgineModuleInstance.class);
        NewOrderEndpoint endpoint = new NewOrderEndpoint(moduleInstance, Clock.systemUTC());

        Session session = mock(Session.class);
        Transaction tx = mock(Transaction.class);
        when(session.beginTransaction()).thenReturn(tx);
        IServerInstance serverInstance = mock(IServerInstance.class);
        when(serverInstance.getDatabaseSession()).thenReturn(session);
        EventBus eventBus = mock(EventBus.class);
        when(serverInstance.getEventBus()).thenReturn(eventBus);

        AcmeAccount account = new AcmeAccount();
        List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> identifiers = new ArrayList<>();
        de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier identifier = new de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier("dns", "example.com");
        identifier.setAuthorizationId("auth");
        identifiers.add(identifier);

        Method m = NewOrderEndpoint.class.getDeclaredMethod("persistOrder", String.class, String.class, AcmeAccount.class, List.class, Instant.class, Instant.class, IServerInstance.class);
        m.setAccessible(true);
        Instant start = Instant.now();
        Instant end = start.plusSeconds(60);
        Object orderObj = m.invoke(endpoint, "order1", "cert1", account, identifiers, start, end, serverInstance);

        verify(session).persist(any(de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier.class));
        verify(session).persist(any(de.morihofi.certgine.acme.types.entities.AcmeOrder.class));
        verify(tx).commit();
        verify(eventBus).publish(any(NewAcmeOrderEvent.class));
        assertNotNull(orderObj);
    }

    @Test
    void buildNewOrderResponseContainsFinalizeUrl() throws Exception {
        CertgineModuleInstance moduleInstance = mock(CertgineModuleInstance.class);
        NewOrderEndpoint endpoint = new NewOrderEndpoint(moduleInstance, Clock.systemUTC());
        AcmeProvisioner provisioner = mock(AcmeProvisioner.class);
        IServerInstance serverInstance = mock(IServerInstance.class);
        when(provisioner.getAcmeApiURL(serverInstance)).thenReturn("https://acme.test");

        de.morihofi.certgine.acme.types.entities.AcmeOrder order = new de.morihofi.certgine.acme.types.entities.AcmeOrder();
        order.setOrderId("order1");
        order.setExpires(Instant.now());
        order.setNotBefore(Instant.now());
        order.setNotAfter(Instant.now());

        List<AcmeOrderIdentifier> respIdentifiers = List.of(new AcmeOrderIdentifier("dns", "example.com"));
        List<String> authz = List.of("url");

        Method m = NewOrderEndpoint.class.getDeclaredMethod("buildNewOrderResponse", AcmeProvisioner.class, IServerInstance.class, de.morihofi.certgine.acme.types.entities.AcmeOrder.class, List.class, List.class);
        m.setAccessible(true);
        var response = (de.morihofi.certgine.acme.servlets.handlerapi.endpoints.objects.NewOrderResponse) m.invoke(endpoint, provisioner, serverInstance, order, respIdentifiers, authz);

        assertEquals("pending", response.getStatus());
        assertEquals("url", response.getAuthorizations().get(0));
        assertTrue(response.getFinalize().contains("/acme/order/order1/finalize"));
    }
}
