/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order;

import de.morihofi.certgine.acme.AcmeModule;
import de.morihofi.certgine.acme.AcmeModuleInstance;
import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.enums.AcmeOrderState;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.Router;
import de.morihofi.certgine.server.common.intf.testing.MockRequest;
import de.morihofi.certgine.server.common.intf.testing.MockResponse;
import de.morihofi.certgine.core.util.DummyServerInstance;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadCsrException;
import de.morihofi.certgine.types.exception.exceptions.ACMEUnauthorizedException;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.base64.Base64Tools;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinalizeOrderEndpointTest {
    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static AcmeModuleInstance moduleInstance() {
        return new AcmeModuleInstance(new AcmeModule(new DummyServerInstance()));
    }

    private static AcmeOrderIdentifierChallenge challengeWithStatus(AcmeOrderIdentifier id, AcmeStatus status) {
        AcmeOrderIdentifierChallenge c = new AcmeOrderIdentifierChallenge();
        c.setStatus(status);
        c.setIdentifier(id);
        return c;
    }

    private static String createCsr(String domain) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        X500Name subject = new X500Name("CN=" + domain);
        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        GeneralName[] names = new GeneralName[]{new GeneralName(GeneralName.dNSName, domain)};
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
        builder.addAttribute(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        PKCS10CertificationRequest csr = builder.build(signer);
        return Base64Tools.base64UrlEncode(csr.getEncoded());
    }

    @Test
    @DisplayName("verifyAuthorizationsComplete throws when any identifier invalid")
    void testVerifyAuthorizationsIncomplete() {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        AcmeOrderIdentifier id1 = new AcmeOrderIdentifier("dns", "example.com");
        AcmeOrderIdentifier id2 = new AcmeOrderIdentifier("dns", "example.org");
        id1.setChallenges(List.of(challengeWithStatus(id1, AcmeStatus.VALID)));
        id2.setChallenges(List.of(challengeWithStatus(id2, AcmeStatus.PENDING)));

        java.lang.reflect.Method m;
        try {
            m = FinalizeOrderEndpoint.class.getDeclaredMethod("verifyAuthorizationsComplete", List.class);
            m.setAccessible(true);
            java.lang.reflect.InvocationTargetException ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> m.invoke(endpoint, List.of(id1, id2)));
            assertInstanceOf(ACMEUnauthorizedException.class, ex.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("verifyAuthorizationsComplete passes when all identifiers valid")
    void testVerifyAuthorizationsComplete() {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        AcmeOrderIdentifier id1 = new AcmeOrderIdentifier("dns", "example.com");
        AcmeOrderIdentifier id2 = new AcmeOrderIdentifier("dns", "example.org");
        id1.setChallenges(List.of(challengeWithStatus(id1, AcmeStatus.VALID)));
        id2.setChallenges(List.of(challengeWithStatus(id2, AcmeStatus.VALID)));

        java.lang.reflect.Method m;
        try {
            m = FinalizeOrderEndpoint.class.getDeclaredMethod("verifyAuthorizationsComplete", List.class);
            m.setAccessible(true);
            assertDoesNotThrow(() -> m.invoke(endpoint, List.of(id1, id2)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("parseCsr rejects invalid CSR")
    void testParseCsrInvalid() throws Exception {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        java.lang.reflect.Method m = FinalizeOrderEndpoint.class.getDeclaredMethod("parseCsr", String.class);
        m.setAccessible(true);
        java.lang.reflect.InvocationTargetException ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> m.invoke(endpoint, "invalid"));
        assertInstanceOf(ACMEBadCsrException.class, ex.getCause());
    }

    @Test
    @DisplayName("parseCsr accepts valid CSR")
    void testParseCsrValid() throws Exception {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        String csr = createCsr("example.com");
        java.lang.reflect.Method m = FinalizeOrderEndpoint.class.getDeclaredMethod("parseCsr", String.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(endpoint, csr));
    }

    @Test
    @DisplayName("verifyCsrAndAuthorizations fails for incomplete authorization")
    void testVerifyCsrAndAuthorizationsInvalid() throws Exception {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        String csr = createCsr("example.com");
        AcmeOrderIdentifier id = new AcmeOrderIdentifier("dns", "example.com");
        id.setChallenges(List.of(challengeWithStatus(id, AcmeStatus.PENDING)));
        java.lang.reflect.Method m = FinalizeOrderEndpoint.class.getDeclaredMethod("verifyCsrAndAuthorizations", String.class, List.class);
        m.setAccessible(true);
        java.lang.reflect.InvocationTargetException ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> m.invoke(endpoint, csr, List.of(id)));
        assertInstanceOf(ACMEUnauthorizedException.class, ex.getCause());
    }

    @Test
    @DisplayName("verifyCsrAndAuthorizations passes for valid CSR and identifiers")
    void testVerifyCsrAndAuthorizationsValid() throws Exception {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        String csr = createCsr("example.com");
        AcmeOrderIdentifier id = new AcmeOrderIdentifier("dns", "example.com");
        id.setChallenges(List.of(challengeWithStatus(id, AcmeStatus.VALID)));
        java.lang.reflect.Method m = FinalizeOrderEndpoint.class.getDeclaredMethod("verifyCsrAndAuthorizations", String.class, List.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(endpoint, csr, List.of(id)));
    }

    @Test
    @DisplayName("issueCertificate returns processing when async flag set")
    void testIssueCertificateAsync() throws Exception {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        AcmeOrder order = new AcmeOrder();
        order.setOrderId("o1");
        AcmeAccount account = new AcmeAccount();
        order.setAccount(account);
        AcmeProvisioner provisioner = new AcmeProvisioner();
        IServerInstance server = Mockito.mock(IServerInstance.class);
        Session session = Mockito.mock(Session.class);
        Transaction tx = Mockito.mock(Transaction.class);
        when(server.getDatabaseSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);
        when(server.getStartupFlags()).thenReturn(Set.of(de.morihofi.certgine.types.server.StartupFlag.USE_ASYNC_CERTIFICATE_ISSUING));
        EventBus bus = new EventBus();
        when(server.getEventBus()).thenReturn(bus);
        java.lang.reflect.Method m = FinalizeOrderEndpoint.class.getDeclaredMethod("issueCertificate", AcmeOrder.class, String.class, AcmeProvisioner.class, IServerInstance.class);
        m.setAccessible(true);
        Object resp = m.invoke(endpoint, order, "csr", provisioner, server);
        assertEquals(AcmeStatus.PROCESSING.getRfcName(), ((de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.objects.AcmeOrderResponse) resp).getStatus());
        verify(session).merge(order);
        verify(tx).commit();
    }

    @Test
    @DisplayName("constructResponse sets headers and body")
    void testConstructResponse() throws Exception {
        FinalizeOrderEndpoint endpoint = new FinalizeOrderEndpoint(moduleInstance());
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        Router router = new Router();
        HandlerContext ctx = new HandlerContext(req, resp, router);
        AcmeProvisioner provisioner = new AcmeProvisioner();
        provisioner.setName("prov");
        IServerInstance server = Mockito.mock(IServerInstance.class);
        Session session = Mockito.mock(Session.class);
        Transaction tx = Mockito.mock(Transaction.class);
        when(server.getServerURL()).thenReturn("https://server");
        when(server.getDatabaseSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);
        when(server.getEventBus()).thenReturn(new EventBus());
        List<AcmeOrderIdentifier> ids = List.of(new AcmeOrderIdentifier("dns", "example.com"));
        List<String> auths = List.of("auth1");
        de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.objects.AcmeOrderResponse response = new de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.objects.AcmeOrderResponse();
        java.lang.reflect.Method m = FinalizeOrderEndpoint.class.getDeclaredMethod("constructResponse", HandlerContext.class, AcmeProvisioner.class, IServerInstance.class, de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.objects.AcmeOrderResponse.class, List.class, List.class, String.class);
        m.setAccessible(true);
        m.invoke(endpoint, ctx, provisioner, server, response, ids.stream().map(i -> new de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier(i.getType(), i.getDataValue())).toList(), auths, "1");
        assertEquals("application/json", resp.getHeader("Content-Type"));
        assertNotNull(resp.getHeader("Replay-Nonce"));
        assertTrue(resp.getBodyAsString().contains("finalize"));
    }

}
