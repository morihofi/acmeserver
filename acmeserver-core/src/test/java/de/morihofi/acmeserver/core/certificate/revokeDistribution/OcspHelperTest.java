package de.morihofi.acmeserver.core.certificate.revokeDistribution;
import de.morihofi.acmeserver.types.events.EventBus;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.ocsp.OcspHelper;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisionerDomainNameRestriction;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import de.morihofi.acmeserver.types.database.entities.ProvisionerMeta;
import de.morihofi.acmeserver.types.database.entities.RootCa;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.RsaCertificateAlgorithm;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.DatabaseConfig;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.types.events.EventBus;
import org.hibernate.Transaction;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OcspHelperTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = new CertificateMetadata(cn, "Org", null, "DE");
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }

    private CryptoStoreManager csm;
    private HibernateUtil hu;
    private IServerInstance si;
    private AcmeProvisioner prov;

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
        prov.setIssuedCertificateExpiration(new CertificateExpiration(0, 0, 1));
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
    }

    @AfterEach
    void cleanup() {
        if (hu != null && hu.getSessionFactory() != null) {
            hu.getSessionFactory().close();
        }
    }

    @Test
    @DisplayName("OCSP response GOOD for valid certificate")
    void testOcspGood() throws Exception {
        OCSPResp resp = OcspHelper.processOCSPRequest(BigInteger.ONE, prov, si);
        assertEquals(OCSPResp.SUCCESSFUL, resp.getStatus());
        BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
        assertEquals(org.bouncycastle.cert.ocsp.CertificateStatus.GOOD, basic.getResponses()[0].getCertStatus());
    }

    @Test
    @DisplayName("OCSP response revoked for revoked cert")
    void testOcspRevoked() throws Exception {
        try (Session s = si.getDatabaseSession()) {
            Transaction tx = s.beginTransaction();

            AcmeAccount acc = new AcmeAccount();
            acc.setAccountId("acc");
            acc.setPublicKeyPEM("");
            acc.setEmails(Collections.emptyList());
            acc.setAcmeProvisioner(prov);
            s.persist(acc);

            AcmeOrder order = new AcmeOrder();
            order.setAccount(acc);
            order.setCertificateSerialNumber(BigInteger.TEN);
            order.setRevokeStatusCode(0);
            order.setRevokeTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
            s.persist(order);

            tx.commit();
        }

        OCSPResp resp = OcspHelper.processOCSPRequest(BigInteger.TEN, prov, si);
        BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
        assertTrue(basic.getResponses()[0].getCertStatus() instanceof RevokedStatus);
    }
}
