package de.morihofi.acmeserver.core.api.acme.api.endpoints;

import de.morihofi.acmeserver.core.api.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.core.api.acme.security.NonceManager;
import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.DatabaseConfig;
import de.morihofi.acmeserver.types.config.ServerConfig;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import de.morihofi.acmeserver.types.database.entities.RsaCertificateAlgorithm;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisionerDomainNameRestriction;
import de.morihofi.acmeserver.types.database.entities.ProvisionerMeta;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.events.EventBus;
import okhttp3.OkHttpClient;
import org.hibernate.Session;
import org.hibernate.Transaction;
import io.javalin.http.Context;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEUnauthorizedException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KeyChangeEndpointTest {

    @BeforeAll
    static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    static class DummyNetworkClient implements INetworkClient {
        @Override public OkHttpClient getOkHttpClient() { return new OkHttpClient(); }
        @Override public de.morihofi.acmeserver.types.intf.network.dns.IDoHClient getDoHClient() { return null; }
        @Override public java.util.List<String> getDnsServer() { return Collections.emptyList(); }
        @Override public Proxy getProxy() { return Proxy.NO_PROXY; }
    }

    static class DummyContext implements Context {
        private final String body;
        DummyContext(String body) { this.body = body; }
        static class SimpleResponse implements jakarta.servlet.http.HttpServletResponse {
            @Override public void setStatus(int sc) {}
            // implement methods with empty bodies
            @Override public void addCookie(jakarta.servlet.http.Cookie cookie) {}
            @Override public boolean containsHeader(String s) { return false; }
            @Override public String encodeURL(String s) { return null; }
            @Override public String encodeRedirectURL(String s) { return null; }
            @Override public String encodeUrl(String s) { return null; }
            @Override public String encodeRedirectUrl(String s) { return null; }
            @Override public void sendError(int i, String s) {}
            @Override public void sendError(int i) {}
            @Override public void sendRedirect(String s) {}
            @Override public void setDateHeader(String s, long l) {}
            @Override public void addDateHeader(String s, long l) {}
            @Override public void setHeader(String s, String s1) {}
            @Override public void addHeader(String s, String s1) {}
            @Override public void setIntHeader(String s, int i) {}
            @Override public void addIntHeader(String s, int i) {}
            @Override public void setStatus(int i, String s) {}
            @Override public int getStatus() { return 0; }
            @Override public String getHeader(String s) { return null; }
            @Override public java.util.Collection<String> getHeaders(String s) { return java.util.Collections.emptyList(); }
            @Override public java.util.Collection<String> getHeaderNames() { return java.util.Collections.emptyList(); }
            @Override public java.lang.String getCharacterEncoding() { return null; }
            @Override public java.lang.String getContentType() { return null; }
            @Override public jakarta.servlet.ServletOutputStream getOutputStream() { return null; }
            @Override public java.io.PrintWriter getWriter() { return new java.io.PrintWriter(System.out); }
            @Override public void setCharacterEncoding(java.lang.String s) {}
            @Override public void setContentLength(int i) {}
            @Override public void setContentLengthLong(long l) {}
            @Override public void setContentType(java.lang.String s) {}
            @Override public void setBufferSize(int i) {}
            @Override public int getBufferSize() { return 0; }
            @Override public void flushBuffer() {}
            @Override public void resetBuffer() {}
            @Override public boolean isCommitted() { return false; }
            @Override public void reset() {}
            @Override public void setLocale(java.util.Locale locale) {}
            @Override public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
        }
        @Override public String body() { return body; }
        @Override public jakarta.servlet.http.HttpServletRequest req() { return null; }
        @Override public jakarta.servlet.http.HttpServletResponse res() { return new SimpleResponse(); }
        @Override public io.javalin.http.HandlerType handlerType() { return null; }
        @Override public String matchedPath() { return null; }
        @Override public String endpointHandlerPath() { return null; }
        @Override public <T> T appData(io.javalin.config.Key<T> key) { return null; }
        @Override public io.javalin.json.JsonMapper jsonMapper() { return null; }
        @Override public <T> T with(Class<? extends io.javalin.plugin.ContextPlugin<?, T>> plugin) { return null; }
        @Override public boolean strictContentTypes() { return false; }
        @Override public String pathParam(String s) { return "provisioner".equals(s) ? "default" : null; }
        @Override public java.util.Map<String, String> pathParamMap() { return Collections.emptyMap(); }
        @Override public jakarta.servlet.ServletOutputStream outputStream() { return null; }
        @Override public Context minSizeForCompression(int i) { return this; }
        @Override public Context result(java.io.InputStream inputStream) { return this; }
        @Override public java.io.InputStream resultInputStream() { return null; }
        @Override public void future(java.util.function.Supplier<? extends java.util.concurrent.CompletableFuture<?>> supplier) { }
        @Override public void redirect(String s, io.javalin.http.HttpStatus httpStatus) { }
        @Override public void writeJsonStream(java.util.stream.Stream<?> stream) { }
        @Override public Context skipRemainingHandlers() { return this; }
        @Override public java.util.Set<io.javalin.security.RouteRole> routeRoles() { return Collections.emptySet(); }
    }

    static IServerInstance createServerInstance(String dbName, Path tempDir) throws Exception {
        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);
        ServerConfig sc = new ServerConfig();
        sc.setDnsName("example.com");
        cfg.setServer(sc);

        EventBus bus = new EventBus();
        HibernateUtil hu = new HibernateUtil(cfg, true, bus);
        NonceManager nm = new NonceManager(hu, true, bus);

        // create basic provisioner and root CA
        try (Session s = hu.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            RootCa root = new RootCa();
            root.setCertificateConfig(new CertificateConfig(new CertificateMetadata("","","",""), new CertificateExpiration(0,0,1), new RsaCertificateAlgorithm(2048)));
            root.setInternalUuid("root");
            s.persist(root);

            AcmeProvisioner prov = new AcmeProvisioner();
            prov.setName("default");
            prov.setRootCa(root);
            prov.setMeta(new ProvisionerMeta("", ""));
            prov.setCertificateConfig(new CertificateConfig(new CertificateMetadata("","","",""), new CertificateExpiration(0,0,1), new RsaCertificateAlgorithm(2048)));
            prov.setIssuedCertificateExpiration(new CertificateExpiration(0,3,0));
            prov.setWildcardAllowed(false);
            prov.setIpAllowed(true);
            AcmeProvisionerDomainNameRestriction r = new AcmeProvisionerDomainNameRestriction();
            r.setEnabled(false);
            r.setMustEndWith(Collections.emptyList());
            prov.setAcmeProvisionerDomainNameRestriction(r);
            s.persist(prov);
            tx.commit();
        }

        return new IServerInstance() {
            @Override public String getServerURL() { return "https://example.com"; }
            @Override public Session getDatabaseSession() { return hu.getSessionFactory().openSession(); }
            @Override public ICryptoStoreManager getCryptoStoreManager() { return null; }
            @Override public Config getAppConfig() { return cfg; }
            @Override public INonceManager getNonceManager() { return nm; }
            @Override public RootCa getRootCa() { return new RootCa(); }
            @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
            @Override public INetworkClient getNetworkClient() { return new DummyNetworkClient(); }
            @Override public EventBus getEventBus() { return new EventBus(); }
        };
    }

    @Test
    @DisplayName("Constructor creates instance")
    void testConstructor() throws Exception {
        Path temp = Files.createTempDirectory("srv");
        IServerInstance si = createServerInstance("const", temp);
        KeyChangeEndpoint endpoint = new KeyChangeEndpoint(si);
        assertNotNull(endpoint);
        assertInstanceOf(AbstractAcmeEndpoint.class, endpoint);
    }

    private static String buildRequest(IServerInstance si, AcmeProvisioner prov, String accountId,
                                        KeyPair oldKey, KeyPair newKey, boolean useValidOldKey) throws Exception {
        Gson g = new Gson();

        PublicJsonWebKey newJwk = PublicJsonWebKey.Factory.newPublicJwk(newKey.getPublic());
        PublicJsonWebKey oldJwk = PublicJsonWebKey.Factory.newPublicJwk(useValidOldKey ? oldKey.getPublic() : newKey.getPublic());

        JsonObject innerPayload = new JsonObject();
        innerPayload.addProperty("account", prov.getAcmeApiURL(si) + "/acme/acct/" + accountId);
        innerPayload.add("oldKey", JsonParser.parseString(oldJwk.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY)));

        JsonWebSignature inner = new JsonWebSignature();
        inner.setPayload(g.toJson(innerPayload));
        inner.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        inner.setJwkHeader(newJwk);
        inner.setKey(newKey.getPrivate());
        inner.sign();
        String[] innerParts = inner.getCompactSerialization().split("\\.");
        String innerJson = String.format("{\"protected\":\"%s\",\"payload\":\"%s\",\"signature\":\"%s\"}", innerParts[0], innerParts[1], innerParts[2]);

        JsonWebSignature outer = new JsonWebSignature();
        outer.setPayload(innerJson);
        outer.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        outer.setKey(oldKey.getPrivate());
        outer.setKeyIdHeaderValue(prov.getAcmeApiURL(si) + "/acme/acct/" + accountId);
        outer.setHeader("nonce", "testnonce");
        outer.sign();
        String[] outerParts = outer.getCompactSerialization().split("\\.");
        return String.format("{\"protected\":\"%s\",\"payload\":\"%s\",\"signature\":\"%s\"}", outerParts[0], outerParts[1], outerParts[2]);
    }

    @Test
    @DisplayName("Key rollover updates account key")
    void testKeyRolloverSuccess() throws Exception {
        Path temp = Files.createTempDirectory("srv");
        try {
            IServerInstance si = createServerInstance("succ", temp);
            AcmeProvisioner prov = AcmeProvisioner.getForName(si, "default");

            KeyPair oldKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            String oldPem = PemUtil.convertToPem(oldKey.getPublic());
            KeyPair newKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();

            String accountId = UUID.randomUUID().toString();
            AcmeAccount acc = new AcmeAccount();
            acc.setAccountId(accountId);
            acc.setPublicKeyPEM(oldPem);
            acc.setEmails(Collections.emptyList());
            acc.setAcmeProvisioner(prov);
            try (Session s = si.getDatabaseSession()) {
                Transaction tx = s.beginTransaction();
                s.persist(acc);
                tx.commit();
            }

            String body = buildRequest(si, prov, accountId, oldKey, newKey, true);
            Context ctx = new DummyContext(body);
            KeyChangeEndpoint endpoint = new KeyChangeEndpoint(si);

            assertDoesNotThrow(() -> endpoint.handle(ctx));

            try (Session s = si.getDatabaseSession()) {
                AcmeAccount updated = s.get(AcmeAccount.class, acc.getId());
                assertEquals(PemUtil.convertToPem(newKey.getPublic()), updated.getPublicKeyPEM());
            }
        } finally {
            // ignore cleanup
        }
    }

    @Test
    @DisplayName("Key rollover fails on mismatched old key")
    void testKeyRolloverOldKeyMismatch() throws Exception {
        Path temp = Files.createTempDirectory("srv");
        try {
            IServerInstance si = createServerInstance("fail", temp);
            AcmeProvisioner prov = AcmeProvisioner.getForName(si, "default");

            KeyPair oldKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            String oldPem = PemUtil.convertToPem(oldKey.getPublic());
            KeyPair newKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();

            String accountId = UUID.randomUUID().toString();
            AcmeAccount acc = new AcmeAccount();
            acc.setAccountId(accountId);
            acc.setPublicKeyPEM(oldPem);
            acc.setEmails(Collections.emptyList());
            acc.setAcmeProvisioner(prov);
            try (Session s = si.getDatabaseSession()) {
                Transaction tx = s.beginTransaction();
                s.persist(acc);
                tx.commit();
            }

            String body = buildRequest(si, prov, accountId, oldKey, newKey, false);
            Context ctx = new DummyContext(body);
            KeyChangeEndpoint endpoint = new KeyChangeEndpoint(si);

            assertThrows(ACMEUnauthorizedException.class, () -> endpoint.handle(ctx));
        } finally {
            // ignore cleanup
        }
    }
}
