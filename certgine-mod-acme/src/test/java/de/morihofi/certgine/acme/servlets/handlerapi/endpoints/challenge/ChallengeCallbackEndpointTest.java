/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.certgine.acme.AcmeModule;
import de.morihofi.certgine.acme.AcmeModuleInstance;
import de.morihofi.certgine.acme.challenges.ChallengeResult;
import de.morihofi.certgine.acme.challenges.HTTPChallenge;
import de.morihofi.certgine.acme.servlets.handlerapi.objects.ACMERequestBody;
import de.morihofi.certgine.acme.types.api.AcmeChallengeType;
import de.morihofi.certgine.acme.types.entities.*;
import de.morihofi.certgine.acme.types.events.AfterChallengeEvent;
import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.Router;
import de.morihofi.certgine.server.common.intf.testing.MockRequest;
import de.morihofi.certgine.server.common.intf.testing.MockResponse;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMEConnectionErrorException;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChallengeCallbackEndpointTest {

    private static ChallengeCallbackEndpoint createEndpoint(EventBus bus, IServerInstance server) {
        AcmeModuleInstance moduleInstance = new AcmeModuleInstance(new AcmeModule(server));
        ChallengeCallbackEndpoint endpoint = spy(new ChallengeCallbackEndpoint(moduleInstance));
        doNothing().when(endpoint).performSignatureAndNonceCheck(any(), any(AcmeAccount.class), any());
        return endpoint;
    }

    private static HandlerContext createContext(ChallengeCallbackEndpoint endpoint) {
        Router router = new Router();
        router.addHandler(new Endpoint(HandlerType.POST, "/acme/chall/{challengeId}/{challengeType}", endpoint));
        MockRequest req = new MockRequest().method("POST");
        req.path("/acme/chall/cid1/http-01");
        MockResponse resp = new MockResponse();
        return new HandlerContext(req, resp, router);
    }

    private static AcmeOrderIdentifierChallenge createChallenge(AcmeAccount account) {
        AcmeOrder order = new AcmeOrder();
        order.setAccount(account);
        AcmeOrderIdentifier identifier = new AcmeOrderIdentifier("dns", "example.com");
        identifier.setOrder(order);
        return new AcmeOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier, "cid1", "tok");
    }

    private static AcmeAccount createAccount() {
        AcmeAccount acc = new AcmeAccount();
        acc.setAccountId("acc1");
        acc.setPublicKeyPEM("pem");
        return acc;
    }

    private static AcmeProvisioner provisioner() {
        AcmeProvisioner p = new AcmeProvisioner();
        p.setName("prov");
        return p;
    }

    private static IServerInstance server(EventBus bus) {
        IServerInstance server = mock(IServerInstance.class);
        when(server.getEventBus()).thenReturn(bus);
        when(server.getServerURL()).thenReturn("https://ca.example.com");
        when(server.getDatabaseSession()).thenReturn(mock(Session.class));
        when(server.getNetworkClient()).thenReturn(mock(INetworkClient.class));
        return server;
    }

    @Test
    @DisplayName("publishes success result")
    void publishesSuccessResult() throws Exception {
        EventBus bus = mock(EventBus.class);
        IServerInstance server = server(bus);
        ChallengeCallbackEndpoint endpoint = createEndpoint(bus, server);
        HandlerContext ctx = createContext(endpoint);
        AcmeAccount account = createAccount();
        AcmeOrderIdentifierChallenge challenge = createChallenge(account);

        try (MockedStatic<AcmeOrderIdentifierChallenge> mockChallenge = mockStatic(AcmeOrderIdentifierChallenge.class);
             MockedStatic<HTTPChallenge> mockHttp = mockStatic(HTTPChallenge.class);
             MockedStatic<AcmeHttpNonce> mockNonce = mockStatic(AcmeHttpNonce.class)) {
            mockChallenge.when(() -> AcmeOrderIdentifierChallenge.getACMEIdentifierChallenge("cid1", server))
                    .thenReturn(challenge);
            mockChallenge.when(() -> AcmeOrderIdentifierChallenge.passChallenge("cid1", server)).thenAnswer(i -> null);
            mockChallenge.when(() -> AcmeOrderIdentifierChallenge.failChallenge("cid1", server)).thenAnswer(i -> null);
            mockNonce.when(() -> AcmeHttpNonce.createNonce(server)).thenReturn("n");
            mockHttp.when(() -> HTTPChallenge.check("tok", "example.com", account, server))
                    .thenReturn(new ChallengeResult(true, ""));

            endpoint.handleRequest(ctx, provisioner(), new Gson(), new ACMERequestBody());

            ArgumentCaptor<AfterChallengeEvent> captor = ArgumentCaptor.forClass(AfterChallengeEvent.class);
            verify(bus).publish(captor.capture());
            AfterChallengeEvent event = captor.getValue();
            assertEquals(AcmeChallengeType.HTTP_01, event.getMethod());
            assertEquals("cid1", event.getChallengeId());
            assertTrue(event.getResult().successful());
        }
    }

    @Test
    @DisplayName("publishes failure result")
    void publishesFailureResult() throws Exception {
        EventBus bus = mock(EventBus.class);
        IServerInstance server = server(bus);
        ChallengeCallbackEndpoint endpoint = createEndpoint(bus, server);
        HandlerContext ctx = createContext(endpoint);
        AcmeAccount account = createAccount();
        AcmeOrderIdentifierChallenge challenge = createChallenge(account);

        try (MockedStatic<AcmeOrderIdentifierChallenge> mockChallenge = mockStatic(AcmeOrderIdentifierChallenge.class);
             MockedStatic<HTTPChallenge> mockHttp = mockStatic(HTTPChallenge.class);
             MockedStatic<AcmeHttpNonce> mockNonce = mockStatic(AcmeHttpNonce.class)) {
            mockChallenge.when(() -> AcmeOrderIdentifierChallenge.getACMEIdentifierChallenge("cid1", server))
                    .thenReturn(challenge);
            mockChallenge.when(() -> AcmeOrderIdentifierChallenge.passChallenge("cid1", server)).thenAnswer(i -> null);
            mockChallenge.when(() -> AcmeOrderIdentifierChallenge.failChallenge("cid1", server)).thenAnswer(i -> null);
            mockNonce.when(() -> AcmeHttpNonce.createNonce(server)).thenReturn("n");
            mockHttp.when(() -> HTTPChallenge.check("tok", "example.com", account, server))
                    .thenReturn(new ChallengeResult(false, "fail"));

            assertThrows(ACMEConnectionErrorException.class,
                    () -> endpoint.handleRequest(ctx, provisioner(), new Gson(), new ACMERequestBody()));

            ArgumentCaptor<AfterChallengeEvent> captor = ArgumentCaptor.forClass(AfterChallengeEvent.class);
            verify(bus).publish(captor.capture());
            AfterChallengeEvent event = captor.getValue();
            assertFalse(event.getResult().successful());
            assertEquals("fail", event.getResult().errorReason());
        }
    }
}

