package de.morihofi.acmeserver.revocation.endpoints;

import de.morihofi.acmeserver.server.common.intf.*;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.cryptography.ocsp.OcspProcessor;
import de.morihofi.acmeserver.types.httpserver.HandlerType;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OcspEndpointTest {

    static class StubRequest implements Request {
        String path;
        String method = "GET";
        Map<String,String> query = new HashMap<>();
        Map<String,String> headers = new HashMap<>();
        byte[] body = new byte[0];
        @Override public jakarta.servlet.http.HttpServletRequest getHttpServletRequest(){return null;}
        @Override public String getPath(){return path;}
        @Override public String getMethod(){return method;}
        @Override public String getHeader(String name){return headers.get(name);} 
        @Override public String getBody(){return new String(body, StandardCharsets.UTF_8);} 
        @Override public String getIP(){return "127.0.0.1";} 
        @Override public String getQueryParam(String name){return query.get(name);} 
        @Override public byte[] getBodyBytes(){return body;}
    }

    static class StubResponse extends Response {
        Map<String,String> headers = new HashMap<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @Override public void setHeader(String name, String value){headers.put(name,value);} 
        @Override public String getHeader(String name){return headers.get(name);} 
        @Override public void setBodyBytes(byte[] data){try{out.write(data);}catch(IOException ignore){}} 
        @Override public Map<String,String> getHeaders(){return headers;} 
        @Override public java.io.OutputStream getOutputStream(){return out;}
    }

    @BeforeAll
    static void setup(){
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
        StubRequest req = new StubRequest();
        StubResponse resp = new StubResponse();
        KeyPair kp = de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = de.morihofi.acmeserver.cryptography.certificate.X509Generator.generate(
                de.morihofi.acmeserver.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.acmeserver.cryptography.certificate.X509Generator.Type.ROOT_CA)
                        .certificateConfig(new CertificateConfig(
                                CertificateMetadata.builder()
                                        .commonName("CA")
                                        .organisation("Org")
                                        .countryCode("DE")
                                        .build(),
                                new CertificateExpiration(0,0,1), null))
                        .ownKeyPair(kp)
                        .build());
        OCSPReq ocspReq = createReq(BigInteger.ONE, cert);
        String encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ocspReq.getEncoded());
        req.path = "/revocation/p/ocsp/" + URLEncoder.encode(encoded, StandardCharsets.UTF_8);
        HandlerContext ctx = new HandlerContext(req, resp, router);
        AcmeProvisioner prov = new AcmeProvisioner(); prov.setName("p");
        try (MockedStatic<AcmeProvisioner> mockProv = Mockito.mockStatic(AcmeProvisioner.class);
             MockedStatic<OcspProcessor> mockProc = Mockito.mockStatic(OcspProcessor.class)) {
            mockProv.when(() -> AcmeProvisioner.getForName(handler.serverInstance, "p")).thenReturn(prov);
            mockProc.when(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, prov, handler.serverInstance))
                    .thenReturn(new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, null));
            handler.handle(ctx);
            mockProc.verify(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, prov, handler.serverInstance));
            assertEquals("application/ocsp-response", resp.getHeader("Content-Type"));
            assertTrue(resp.out.size() > 0);
        }
    }

    @Test
    @DisplayName("POST endpoint delegates request")
    void testPost() throws Exception {
        Router router = new Router();
        OcspEndpointPost handler = new OcspEndpointPost(Mockito.mock(IServerInstance.class));
        router.addHandler(new Endpoint(HandlerType.POST, "/revocation/{provisioner}/ocsp", handler));
        StubRequest req = new StubRequest();
        StubResponse resp = new StubResponse();
        req.method = "POST";
        KeyPair kp = de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate cert = de.morihofi.acmeserver.cryptography.certificate.X509Generator.generate(
                de.morihofi.acmeserver.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.acmeserver.cryptography.certificate.X509Generator.Type.ROOT_CA)
                        .certificateConfig(new CertificateConfig(
                                CertificateMetadata.builder()
                                        .commonName("CA")
                                        .organisation("Org")
                                        .countryCode("DE")
                                        .build(),
                                new CertificateExpiration(0,0,1), null))
                        .ownKeyPair(kp)
                        .build());
        OCSPReq ocspReq = createReq(BigInteger.ONE, cert);
        req.body = ocspReq.getEncoded();
        req.path = "/revocation/p/ocsp";
        HandlerContext ctx = new HandlerContext(req, resp, router);
        AcmeProvisioner prov = new AcmeProvisioner(); prov.setName("p");
        try (MockedStatic<AcmeProvisioner> mockProv = Mockito.mockStatic(AcmeProvisioner.class);
             MockedStatic<OcspProcessor> mockProc = Mockito.mockStatic(OcspProcessor.class)) {
            mockProv.when(() -> AcmeProvisioner.getForName(handler.serverInstance, "p")).thenReturn(prov);
            mockProc.when(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, prov, handler.serverInstance))
                    .thenReturn(new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, null));
            handler.handle(ctx);
            mockProc.verify(() -> OcspProcessor.processOCSPRequest(BigInteger.ONE, prov, handler.serverInstance));
            assertEquals("application/ocsp-response", resp.getHeader("Content-Type"));
            assertTrue(resp.out.size() > 0);
        }
    }
}
