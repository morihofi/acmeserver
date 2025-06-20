package de.morihofi.acmeserver.acme.challenges;
import de.morihofi.acmeserver.types.events.EventBus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.acme.challenges.HTTPChallenge;
import de.morihofi.acmeserver.acme.challenges.ChallengeResult;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import okhttp3.OkHttpClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import lombok.NonNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class HTTPChallengeTest {

    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }
    static class DummyNetworkClient implements INetworkClient {
        private final OkHttpClient client = new OkHttpClient();
        @Override public OkHttpClient getOkHttpClient() { return client; }
        @Override public de.morihofi.acmeserver.types.intf.network.dns.IDoHClient getDoHClient() { return null; }
        @Override public java.util.List<String> getDnsServer() { return Collections.emptyList(); }
        @Override public Proxy getProxy() { return Proxy.NO_PROXY; }
    }

    static class DummyServerInstance implements IServerInstance {
        private final INetworkClient net = new DummyNetworkClient();
        private final BuildMetadata meta = BuildMetadata.builder().buildVersion("test").gitCommit("abc").build();
        @NonNull
        @Override public String getServerURL() { return ""; }
        @NonNull
        @Override public org.hibernate.Session getDatabaseSession() { return null; }
        @NonNull
        @Override public ICryptoStoreManager getCryptoStoreManager() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @NonNull
        @Override public INonceManager getNonceManager() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @NonNull
        @Override public de.morihofi.acmeserver.types.database.entities.TsaAuthority getTsaAuthority() { return null; }
        @NonNull
        @Override public BuildMetadata getBuildMetadata() { return meta; }
        @NonNull
        @Override public INetworkClient getNetworkClient() { return net; }
        @NonNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @NonNull
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
    }

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void setHandler(String path, String body) {
        server.createContext(path, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(200, body.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body.getBytes());
                }
            }
        });
    }

    @Test
    @DisplayName("check returns success for valid token")
    void testCheckValid() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        String token = "token123";

        AcmeAccount account = new AcmeAccount();
        account.setPublicKeyPEM(PemUtil.convertToPem(kp.getPublic()));

        String expected = de.morihofi.acmeserver.cryptography.acme.AcmeTokenCryptography.keyAuthorizationFor(token, kp.getPublic());
        setHandler("/.well-known/acme-challenge/" + token, expected);

        ChallengeResult result = HTTPChallenge.check(token, "localhost:" + port, account, new DummyServerInstance());
        assertTrue(result.successful());
    }

    @Test
    @DisplayName("check fails on invalid token")
    void testCheckInvalid() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        String token = "token123";

        AcmeAccount account = new AcmeAccount();
        account.setPublicKeyPEM(PemUtil.convertToPem(kp.getPublic()));

        setHandler("/.well-known/acme-challenge/" + token, "wrong");

        ChallengeResult result = HTTPChallenge.check(token, "localhost:" + port, account, new DummyServerInstance());
        assertFalse(result.successful());
        assertNotNull(result.errorReason());
    }
}

