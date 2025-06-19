package de.morihofi.acmeserver.acme.api.endpoints;

import de.morihofi.acmeserver.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.acme.api.endpoints.KeyChangeEndpoint;
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
import org.jetbrains.annotations.NotNull;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEUnauthorizedException;
import de.morihofi.acmeserver.server.common.intf.Endpoint;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.server.common.intf.Router;
import de.morihofi.acmeserver.types.httpserver.HandlerType;
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
        INonceManager nm = decoded -> {};

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
            @NotNull
            @Override public String getServerURL() { return "https://example.com"; }
            @NotNull
            @Override public Session getDatabaseSession() { return hu.getSessionFactory().openSession(); }
            @NotNull
            @Override public ICryptoStoreManager getCryptoStoreManager() { return null; }
            @NotNull
            @Override public Config getAppConfig() { return cfg; }
            @NotNull
            @Override public INonceManager getNonceManager() { return nm; }
            @NotNull
            @Override public RootCa getRootCa() { return new RootCa(); }
            @NotNull
            @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
            @NotNull
            @Override public INetworkClient getNetworkClient() { return new DummyNetworkClient(); }
            @NotNull
        @Override public EventBus getEventBus() { return new EventBus(); }
        @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
        };
    }

    static class DummyRequest implements de.morihofi.acmeserver.server.common.intf.Request {
        private final String path;
        private final String method;
        private final String body;
        DummyRequest(String path, String method, String body) { this.path = path; this.method = method; this.body = body; }
        @Override public String getPath() { return path; }
        @Override public String getMethod() { return method; }
        @Override public String getHeader(String name) { return null; }
        @Override public String getBody() { return body; }
        @Override public String getIP() { return "127.0.0.1"; }
        @Override public String getQueryParam(String name) { return null; }
        @Override public byte[] getBodyBytes() { return body.getBytes(); }
    }

    static class DummyResponse extends de.morihofi.acmeserver.server.common.intf.Response {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        @Override public void setHeader(String name, String value) { }
        @Override public String getHeader(String name) { return null; }
        @Override public void setBodyBytes(byte[] data) { try { out.write(data); } catch (java.io.IOException ignored) {} }
        @Override public java.util.Map<String, String> getHeaders() { return java.util.Collections.emptyMap(); }
        @Override public java.io.OutputStream getOutputStream() { return out; }
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
            DummyRequest req = new DummyRequest("/acme/default/key-change", "POST", body);
            DummyResponse resp = new DummyResponse();
            Router router = new Router();
            router.addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/key-change", new KeyChangeEndpoint(si)));
            HandlerContext ctx = new HandlerContext(req, resp, router);
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
            DummyRequest req = new DummyRequest("/acme/default/key-change", "POST", body);
            DummyResponse resp = new DummyResponse();
            Router router = new Router();
            router.addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/key-change", new KeyChangeEndpoint(si)));
            HandlerContext ctx = new HandlerContext(req, resp, router);
            KeyChangeEndpoint endpoint = new KeyChangeEndpoint(si);

            assertThrows(ACMEUnauthorizedException.class, () -> endpoint.handle(ctx));
        } finally {
            // ignore cleanup
        }
    }
}
