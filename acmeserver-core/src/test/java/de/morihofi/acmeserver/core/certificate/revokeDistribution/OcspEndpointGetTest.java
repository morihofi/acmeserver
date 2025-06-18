package de.morihofi.acmeserver.core.certificate.revokeDistribution;
import de.morihofi.acmeserver.types.events.EventBus;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisionerDomainNameRestriction;
import de.morihofi.acmeserver.types.database.entities.ProvisionerMeta;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.DatabaseConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.acme.revokeDistribution.OcspEndpointGet;
import de.morihofi.acmeserver.core.database.HibernateUtil;
import org.hibernate.Transaction;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.morihofi.acmeserver.server.common.intf.Endpoint;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.server.common.intf.Request;
import de.morihofi.acmeserver.server.common.intf.Response;
import de.morihofi.acmeserver.server.common.intf.Router;
import de.morihofi.acmeserver.types.httpserver.HandlerType;


import java.math.BigInteger;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OcspEndpointGetTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = new CertificateMetadata(cn, "Org", null, "DE");
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }

    static class DummyRequest implements Request {
        private final String path;
        private final String method;
        DummyRequest(String path, String method) { this.path = path; this.method = method; }
        @Override public String getPath() { return path; }
        @Override public String getMethod() { return method; }
        @Override public String getHeader(String name) { return null; }
        @Override public String getBody() { return null; }
        @Override public String getIP() { return "127.0.0.1"; }
        @Override public String getQueryParam(String name) { return null; }
        @Override public byte[] getBodyBytes() { return new byte[0]; }
    }

    static class DummyResponse extends Response {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public void setBodyBytes(byte[] data) { try { body.reset(); body.write(data); } catch (java.io.IOException ignored) {} }
        @Override public java.util.Map<String, String> getHeaders() { return headers; }
        @Override public OutputStream getOutputStream() { return body; }
        byte[] bodyBytes() { return body.toByteArray(); }
    }

    private OcspEndpointGet endpoint;
    private AcmeProvisioner prov;
    private IServerInstance si;
    private CryptoStoreManager csm;
    private HibernateUtil hu;

    @BeforeEach
    void init() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path ksPath = fs.getPath("store.p12");
        csm = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "".toCharArray()));

        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Root"))
                .ownKeyPair(rootKey)
                .build());

        KeyPair interKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate interCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.INTERMEDIATE_CA)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .ownKeyPair(interKey)
                .certificateConfig(cfg("Inter"))
                .build());

        csm.getKeyStore().setKeyEntry(csm.getKeyStoreAliasForProvisionerIntermediate("test"),
                interKey.getPrivate(), "".toCharArray(), new java.security.cert.Certificate[]{interCert});

        Config cfg = new Config();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        db.setUser("sa");
        db.setPassword("");
        cfg.setDatabase(db);

        EventBus bus = new EventBus();
        hu = new HibernateUtil(cfg, true, bus);

        RootCa root = new RootCa();
        root.setCertificateConfig(cfg("root"));
        root.setInternalUuid("root");

        prov = new AcmeProvisioner();
        prov.setName("test");
        prov.setRootCa(root);
        prov.setMeta(new ProvisionerMeta("", ""));
        prov.setCertificateConfig(cfg("inter"));
        prov.setIssuedCertificateExpiration(new CertificateExpiration(0,0,1));
        prov.setWildcardAllowed(false);
        prov.setIpAllowed(true);
        AcmeProvisionerDomainNameRestriction r = new AcmeProvisionerDomainNameRestriction();
        r.setEnabled(false);
        r.setMustEndWith(Collections.emptyList());
        prov.setAcmeProvisionerDomainNameRestriction(r);

        try (Session s = hu.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            s.persist(root);
            s.persist(prov);
            tx.commit();
        }

        si = new IServerInstance() {
            @NotNull
            @Override public String getServerURL() { return "https://example.com"; }
            @NotNull
            @Override public Session getDatabaseSession() { return hu.getSessionFactory().openSession(); }
            @NotNull
            @Override public ICryptoStoreManager getCryptoStoreManager() { return csm; }
            @NotNull
            @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return cfg; }
            @NotNull
            @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
            @NotNull
            @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return root; }
            @NotNull
            @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
            @NotNull
            @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
            @NotNull
            @Override public EventBus getEventBus() { return bus; }
            @NotNull
            @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() { return java.util.Collections.emptySet(); }
        };

        endpoint = new OcspEndpointGet(si);
    }

    @AfterEach
    void cleanup() {
        if (hu != null && hu.getSessionFactory() != null) {
            hu.getSessionFactory().close();
        }
    }

    @Test
    @DisplayName("handle decodes url-encoded request")
    void testHandleDecodes() throws Exception {
        KeyPair interKey = si.getCryptoStoreManager().getIntermediateCerificateAuthorityKeyPair("test");
        X509Certificate interCert = si.getCryptoStoreManager().getX509CertificateForProvisioner("test");

        DigestCalculator dig = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        CertificateID certId = new CertificateID(dig, new JcaX509CertificateHolder(interCert), BigInteger.ONE);
        OCSPReq req = new OCSPReqBuilder().addRequest(certId).build();
        String b64 = java.util.Base64.getEncoder().encodeToString(req.getEncoded());
        String encoded = URLEncoder.encode(b64, StandardCharsets.UTF_8);

        Router router = new Router();
        router.addHandler(new Endpoint(HandlerType.GET, "/acme/{provisioner}/ocsp/{ocspRequest}", endpoint));
        DummyRequest request = new DummyRequest("/acme/test/ocsp/" + encoded, "GET");
        DummyResponse respObj = new DummyResponse();
        HandlerContext ctx = new HandlerContext(request, respObj, router);
        try (MockedStatic<AcmeProvisioner> mock = Mockito.mockStatic(AcmeProvisioner.class)) {
            mock.when(() -> AcmeProvisioner.getForName(si, "test")).thenReturn(prov);
            endpoint.handle(ctx);
        }

        byte[] body = respObj.bodyBytes();
        assertNotNull(body);
        OCSPResp resp = new OCSPResp(body);
        assertEquals(OCSPResp.SUCCESSFUL, resp.getStatus());
    }
}
