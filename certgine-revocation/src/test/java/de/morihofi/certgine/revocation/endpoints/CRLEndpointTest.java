/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.endpoints;

import de.morihofi.certgine.server.common.intf.*;
import de.morihofi.certgine.revocation.crl.CrlStore;
import de.morihofi.certgine.cryptography.crl.CrlGenerator;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CRLEndpointTest {

    static class StubRequest implements Request {
        String path;
        String method = "GET";
        Map<String, String> query = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
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
        Map<String, String> headers = new HashMap<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @Override public void setHeader(String name, String value){headers.put(name,value);}
        @Override public String getHeader(String name){return headers.get(name);}
        @Override public void setBodyBytes(byte[] data){try{out.write(data);}catch(IOException ignore){}}
        @Override public Map<String, String> getHeaders(){return headers;}
        @Override public java.io.OutputStream getOutputStream(){return out;}
    }

    @BeforeAll
    static void setup(){
        Security.addProvider(new BouncyCastleProvider());
    }

    private static X509CRL sampleCrl() throws Exception {
        KeyPair kp = de.morihofi.certgine.cryptography.keys.KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        java.security.cert.X509Certificate cert = de.morihofi.certgine.cryptography.certificate.X509Generator.generate(
                de.morihofi.certgine.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.certgine.cryptography.certificate.X509Generator.Type.ROOT_CA)
                        .certificateConfig(new CertificateConfig(
                                CertificateMetadata.builder()
                                        .commonName("CA")
                                        .organisation("Org")
                                        .countryCode("DE")
                                        .build(),
                                new CertificateExpiration(0,0,1), null))
                        .ownKeyPair(kp)
                        .build());
        return CrlGenerator.generate(
                CrlGenerator.Request.builder()
                        .caCert(cert)
                        .caPrivateKey(kp.getPrivate())
                        .updateMinutes(5)
                        .build());
    }

    @Test
    @DisplayName("GET endpoint returns CRL with headers")
    void testGet() throws Exception {
        Router router = new Router();
        CRLEndpoint handler = new CRLEndpoint(Mockito.mock(IServerInstance.class));
        router.addHandler(new Endpoint(HandlerType.GET, "/revocation/{provisioner}/crl", handler));
        StubRequest req = new StubRequest();
        StubResponse resp = new StubResponse();
        req.path = "/revocation/p/crl";
        HandlerContext ctx = new HandlerContext(req, resp, router);
        X509CRL crl = sampleCrl();
        byte[] expected = crl.getEncoded();
        CrlStore.CrlEntry entry = new CrlStore.CrlEntry(LocalTime.now(), crl);
        try (MockedStatic<CrlStore> mock = Mockito.mockStatic(CrlStore.class)) {
            mock.when(() -> CrlStore.getCrlForProvisioner("p")).thenReturn(entry);
            handler.handle(ctx);
        }
        assertEquals("application/pkix-crl", resp.getHeader("Content-Type"));
        assertEquals(String.valueOf(expected.length), resp.getHeader("Content-Length"));
        assertArrayEquals(expected, resp.out.toByteArray());
    }

    @Test
    @DisplayName("unknown provisioner throws exception")
    void testUnknownProvisioner() {
        Router router = new Router();
        CRLEndpoint handler = new CRLEndpoint(Mockito.mock(IServerInstance.class));
        router.addHandler(new Endpoint(HandlerType.GET, "/revocation/{provisioner}/crl", handler));
        StubRequest req = new StubRequest();
        StubResponse resp = new StubResponse();
        req.path = "/revocation/unknown/crl";
        HandlerContext ctx = new HandlerContext(req, resp, router);
        try (MockedStatic<CrlStore> mock = Mockito.mockStatic(CrlStore.class)) {
            mock.when(() -> CrlStore.getCrlForProvisioner("unknown")).thenThrow(new IllegalArgumentException("unknown"));
            assertThrows(IllegalArgumentException.class, () -> handler.handle(ctx));
        }
    }
}

