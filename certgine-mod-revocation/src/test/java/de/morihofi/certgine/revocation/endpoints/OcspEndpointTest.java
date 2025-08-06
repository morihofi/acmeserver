package de.morihofi.certgine.revocation.endpoints;

import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.cryptography.ocsp.OcspProcessor;
import de.morihofi.certgine.server.common.intf.*;
import de.morihofi.certgine.server.common.intf.testing.MockRequest;
import de.morihofi.certgine.server.common.intf.testing.MockResponse;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class OcspEndpointTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static OCSPReq createReq(BigInteger serial, X509Certificate caCert) throws Exception {
        DigestCalculator digCalc = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        CertificateID certId = new CertificateID(digCalc, new org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(caCert), serial);
        return new OCSPReqBuilder().addRequest(certId).build();
    }

    @Test
    @DisplayName("GET endpoint decodes and delegates request")
    void testGet() throws Exception {
        Router router = new Router();
        OcspEndpointGet handler = new OcspEndpointGet(Mockito.mock(IServerInstance.class));
        router.addHandler(new Endpoint(HandlerType.GET, "/revocation/{provisioner}/ocsp/{ocspRequest}", handler));
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        KeyPair kp = de.morihofi.certgine.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = de.morihofi.certgine.cryptography.certificate.X509Generator.generate(
                de.morihofi.certgine.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.certgine.cryptography.certificate.X509Generator.Type.ROOT_CA)
                        .certificateConfig(new CertificateConfig(
                                CertificateMetadata.builder().commonName("CA").organisation("Org").countryCode("DE").build(),
                                new CertificateExpiration(0, 0, 1), null))
                        .ownKeyPair(kp)
                        .build());
        OCSPReq ocspReq = createReq(BigInteger.ONE, cert);
        String encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ocspReq.getEncoded());
        req.path("/revocation/p/ocsp/" + URLEncoder.encode(encoded, StandardCharsets.UTF_8));
        HandlerContext ctx = new HandlerContext(req, resp, router);
        AcmeProvisioner prov = new AcmeProvisioner();
        prov.setName("p");
        prov.setInternalUuid("u");
        ICryptoStoreManager crypto = Mockito.mock(ICryptoStoreManager.class);
        Mockito.when(handler.serverInstance.getCryptoStoreManager()).thenReturn(crypto);
        Mockito.when(crypto.getIntermediateCertificate("u")).thenReturn(cert);
        Mockito.when(crypto.getIntermediateCertificateAuthorityKeyPair("u")).thenReturn(kp);
        try (MockedStatic<AcmeProvisioner> mockProv = Mockito.mockStatic(AcmeProvisioner.class);
             MockedStatic<AcmeOrder> mockOrder = Mockito.mockStatic(AcmeOrder.class);
             MockedStatic<OcspProcessor> mockProc = Mockito.mockStatic(OcspProcessor.class)) {
            mockProv.when(() -> AcmeProvisioner.getForName(handler.serverInstance, "p")).thenReturn(prov);
            mockOrder.when(() -> AcmeOrder.getRevokedCertificate(BigInteger.ONE, "p", handler.serverInstance)).thenReturn(null);
            mockProc.when(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, null, cert, kp))
                    .thenReturn(new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, null));
            handler.handle(ctx);
            mockProc.verify(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, null, cert, kp));
            assertEquals("application/ocsp-response", resp.getHeader("Content-Type"));
            assertTrue(resp.getBodyAsBytes().length > 0);
        }
    }

    @Test
    @DisplayName("POST endpoint delegates request")
    void testPost() throws Exception {
        Router router = new Router();
        OcspEndpointPost handler = new OcspEndpointPost(Mockito.mock(IServerInstance.class));
        router.addHandler(new Endpoint(HandlerType.POST, "/revocation/{provisioner}/ocsp", handler));
        MockRequest req = new MockRequest().method("POST");
        MockResponse resp = new MockResponse();
        KeyPair kp = de.morihofi.certgine.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = de.morihofi.certgine.cryptography.certificate.X509Generator.generate(
                de.morihofi.certgine.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.certgine.cryptography.certificate.X509Generator.Type.ROOT_CA)
                        .certificateConfig(new CertificateConfig(
                                CertificateMetadata.builder().commonName("CA").organisation("Org").countryCode("DE").build(),
                                new CertificateExpiration(0, 0, 1), null))
                        .ownKeyPair(kp)
                        .build());
        OCSPReq ocspReq = createReq(BigInteger.ONE, cert);
        req.bodyBytes(ocspReq.getEncoded());
        req.path("/revocation/p/ocsp");
        HandlerContext ctx = new HandlerContext(req, resp, router);
        AcmeProvisioner prov = new AcmeProvisioner();
        prov.setName("p");
        prov.setInternalUuid("u");
        ICryptoStoreManager crypto = Mockito.mock(ICryptoStoreManager.class);
        Mockito.when(handler.serverInstance.getCryptoStoreManager()).thenReturn(crypto);
        Mockito.when(crypto.getIntermediateCertificate("u")).thenReturn(cert);
        Mockito.when(crypto.getIntermediateCertificateAuthorityKeyPair("u")).thenReturn(kp);
        try (MockedStatic<AcmeProvisioner> mockProv = Mockito.mockStatic(AcmeProvisioner.class);
             MockedStatic<AcmeOrder> mockOrder = Mockito.mockStatic(AcmeOrder.class);
             MockedStatic<OcspProcessor> mockProc = Mockito.mockStatic(OcspProcessor.class)) {
            mockProv.when(() -> AcmeProvisioner.getForName(handler.serverInstance, "p")).thenReturn(prov);
            mockOrder.when(() -> AcmeOrder.getRevokedCertificate(BigInteger.ONE, "p", handler.serverInstance)).thenReturn(null);
            mockProc.when(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, null, cert, kp))
                    .thenReturn(new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, null));
            handler.handle(ctx);
            mockProc.verify(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, null, cert, kp));
            assertEquals("application/ocsp-response", resp.getHeader("Content-Type"));
            assertTrue(resp.getBodyAsBytes().length > 0);
        }
    }

    @Test
    @DisplayName("GET endpoint rejects empty OCSP request")
    void testGetEmptyOcspRequest() {
        Router router = new Router() {
            @Override
            public String getPathParam(String path, String param) {
                if ("provisioner".equals(param)) {
                    return "p";
                }
                if ("ocspRequest".equals(param)) {
                    return "";
                }
                return super.getPathParam(path, param);
            }
        };
        OcspEndpointGet handler = new OcspEndpointGet(Mockito.mock(IServerInstance.class));
        MockRequest req = new MockRequest().path("/revocation/p/ocsp/");
        MockResponse resp = new MockResponse();
        HandlerContext ctx = new HandlerContext(req, resp, router);
        assertThrows(IllegalArgumentException.class, () -> handler.handle(ctx));
    }

    @Test
    @DisplayName("GET endpoint rejects invalid Base64 request")
    void testGetInvalidBase64() {
        Router router = new Router();
        OcspEndpointGet handler = new OcspEndpointGet(Mockito.mock(IServerInstance.class));
        router.addHandler(new Endpoint(HandlerType.GET, "/revocation/{provisioner}/ocsp/{ocspRequest}", handler));
        MockRequest req = new MockRequest().path("/revocation/p/ocsp/" + URLEncoder.encode("???", StandardCharsets.UTF_8));
        MockResponse resp = new MockResponse();
        HandlerContext ctx = new HandlerContext(req, resp, router);
        assertThrows(IllegalArgumentException.class, () -> handler.handle(ctx));
    }

    @Test
    @DisplayName("GET endpoint throws when provisioner is unknown")
    void testGetUnknownProvisioner() throws Exception {
        Router router = new Router();
        OcspEndpointGet handler = new OcspEndpointGet(Mockito.mock(IServerInstance.class));
        router.addHandler(new Endpoint(HandlerType.GET, "/revocation/{provisioner}/ocsp/{ocspRequest}", handler));
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        KeyPair kp = de.morihofi.certgine.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = de.morihofi.certgine.cryptography.certificate.X509Generator.generate(
                de.morihofi.certgine.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.certgine.cryptography.certificate.X509Generator.Type.ROOT_CA)
                        .certificateConfig(new CertificateConfig(
                                CertificateMetadata.builder().commonName("CA").organisation("Org").countryCode("DE").build(),
                                new CertificateExpiration(0, 0, 1), null))
                        .ownKeyPair(kp)
                        .build());
        OCSPReq ocspReq = createReq(BigInteger.ONE, cert);
        String encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ocspReq.getEncoded());
        req.path("/revocation/p/ocsp/" + URLEncoder.encode(encoded, StandardCharsets.UTF_8));
        HandlerContext ctx = new HandlerContext(req, resp, router);
        try (MockedStatic<AcmeProvisioner> mockProv = Mockito.mockStatic(AcmeProvisioner.class)) {
            mockProv.when(() -> AcmeProvisioner.getForName(handler.serverInstance, "p")).thenReturn(null);
            assertThrows(NullPointerException.class, () -> handler.handle(ctx));
        }
    }
}
