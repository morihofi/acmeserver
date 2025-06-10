package de.morihofi.acmeserver.core.api.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.morihofi.acmeserver.core.api.acme.security.SignatureCheck;
import io.javalin.config.Key;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.ContextPlugin;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwk.PublicJsonWebKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RevokeCertEndpointTest {

    static class DummyContext implements Context {
        private final String body;
        DummyContext(String body) { this.body = body; }
        @Override public String body() { return body; }
        @Override public HttpServletRequest req() { return null; }
        @Override public HttpServletResponse res() { return null; }
        @Override public HandlerType handlerType() { return null; }
        @Override public String matchedPath() { return null; }
        @Override public String endpointHandlerPath() { return null; }
        @Override public <T> T appData(Key<T> key) { return null; }
        @Override public JsonMapper jsonMapper() { return null; }
        @Override public <T> T with(Class<? extends ContextPlugin<?, T>> plugin) { return null; }
        @Override public boolean strictContentTypes() { return false; }
        @Override public String pathParam(String s) { return null; }
        @Override public Map<String, String> pathParamMap() { return Collections.emptyMap(); }
        @Override public ServletOutputStream outputStream() { return null; }
        @Override public Context minSizeForCompression(int i) { return this; }
        @Override public Context result(InputStream inputStream) { return this; }
        @Override public InputStream resultInputStream() { return null; }
        @Override public void future(Supplier<? extends CompletableFuture<?>> supplier) { }
        @Override public void redirect(String s, HttpStatus httpStatus) { }
        @Override public void writeJsonStream(Stream<?> stream) { }
        @Override public Context skipRemainingHandlers() { return this; }
        @Override public Set<io.javalin.security.RouteRole> routeRoles() { return Collections.emptySet(); }
    }

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

        Context ctx = new DummyContext(body);
        assertDoesNotThrow(() -> SignatureCheck.checkSignature(ctx, kp.getPublic(), new Gson()));
    }
}
