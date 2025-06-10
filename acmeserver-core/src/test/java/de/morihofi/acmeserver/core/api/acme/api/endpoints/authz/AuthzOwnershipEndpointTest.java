package de.morihofi.acmeserver.core.api.acme.api.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.acmeserver.core.api.acme.api.objects.ACMERequestBody;
import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEResourceNotFoundException;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import io.javalin.config.Key;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.ContextPlugin;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuthzOwnershipEndpointTest {

    static class DummyServerInstance implements IServerInstance {
        @Override public String getServerURL() { return ""; }
        @Override public Session getDatabaseSession() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return null; }
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
    }

    @Test
    @DisplayName("Authorization expiration comes from associated order")
    void testAuthorizationExpiration() {
        AuthzOwnershipEndpoint endpoint = new AuthzOwnershipEndpoint(new DummyServerInstance());
        AcmeOrder order = new AcmeOrder();
        Timestamp expires = Timestamp.from(Instant.now().plusSeconds(3600));
        order.setExpires(expires);
        AcmeOrderIdentifier identifier = new AcmeOrderIdentifier("dns", "example.com");
        identifier.setOrder(order);

        assertEquals(expires, endpoint.getAuthorizationExpiration(identifier));
    }

    static class DummyContext implements Context {
        private final String authorizationId;
        DummyContext(String authorizationId) { this.authorizationId = authorizationId; }
        @Override public String pathParam(String s) { return authorizationId; }
        @Override public Context header(String s, String s1) { return this; }
        @Override public Context status(int i) { return this; }
        // Unused methods
        @Override public String body() { return null; }
        @Override public HttpServletRequest req() { return null; }
        @Override public HttpServletResponse res() { return null; }
        @Override public HandlerType handlerType() { return null; }
        @Override public String matchedPath() { return null; }
        @Override public String endpointHandlerPath() { return null; }
        @Override public <T> T appData(Key<T> key) { return null; }
        @Override public JsonMapper jsonMapper() { return null; }
        @Override public <T> T with(Class<? extends ContextPlugin<?, T>> plugin) { return null; }
        @Override public boolean strictContentTypes() { return false; }
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
    @DisplayName("Nonexistent authorization results in 404")
    void testNonexistentAuthorization() {
        AuthzOwnershipEndpoint endpoint = new AuthzOwnershipEndpoint(new DummyServerInstance());
        Context ctx = new DummyContext("authz-unknown");
        ACMEResourceNotFoundException ex = assertThrows(ACMEResourceNotFoundException.class,
                () -> endpoint.handleRequest(ctx, new AcmeProvisioner(), new Gson(), new ACMERequestBody()));
        assertEquals(404, ex.getHttpStatusCode());
    }
}
