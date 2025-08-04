/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints;

import com.google.gson.JsonObject;
import de.morihofi.certgine.acme.security.SignatureCheck;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.Router;
import de.morihofi.certgine.types.json.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwk.PublicJsonWebKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

class RevokeCertEndpointTest {



    @Test
    @DisplayName("Extract account id from kid")
    void testKidParsing() {
        JsonObject obj = new JsonObject();
        obj.addProperty("kid", "https://example.com/acme/acct/abcd-1234");
        assertEquals("abcd-1234", SignatureCheck.getAccountIdFromProtectedKID(obj));
    }

    @Test
    @DisplayName("Verify signature using JWK")
    void testJwkSignature() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(kp.getPublic());

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload("{}");
        jws.setKey(kp.getPrivate());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.setJwkHeader(jwk);
        jws.sign();
        String[] parts = jws.getCompactSerialization().split("\\.");
        String body = String.format("{\"protected\":\"%s\",\"payload\":\"%s\",\"signature\":\"%s\"}", parts[0], parts[1], parts[2]);

        class DummyRequest implements de.morihofi.certgine.server.common.intf.Request {
            @Override
            public HttpServletRequest getHttpServletRequest() {
                return null;
            }

            @Override public String getPath() { return "/"; }
            @Override public String getMethod() { return "POST"; }
            @Override public String getHeader(String name) { return null; }
            @Override public String getBody() { return body; }
            @Override public String getIP() { return "127.0.0.1"; }
            @Override public String getQueryParam(String name) { return null; }
            @Override public byte[] getBodyBytes() { return body.getBytes(); }
        }
        class DummyResponse extends de.morihofi.certgine.server.common.intf.Response {
            @Override public void setHeader(String name, String value) { }
            @Override public String getHeader(String name) { return null; }
            @Override public void setBodyBytes(byte[] data) { }
            @Override public java.util.Map<String, String> getHeaders() { return java.util.Collections.emptyMap(); }
            @Override public java.io.OutputStream getOutputStream() { return new java.io.ByteArrayOutputStream(); }
        }

        HandlerContext ctx = new HandlerContext(new DummyRequest(), new DummyResponse(), new Router());
        assertDoesNotThrow(() -> SignatureCheck.checkSignature(ctx, kp.getPublic(), GsonFactory.createGson()));
    }
}
