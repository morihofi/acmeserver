package de.morihofi.acmeserver.core.certificate.revokeDistribution;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.revoke.CrlGenerator;
import de.morihofi.acmeserver.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.CertificateMetadata;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;

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

        prov = new AcmeProvisioner();
        prov.setName("test");

        si = new IServerInstance() {
            @Override public String getServerURL() { return "https://example.com"; }
            @Override public Session getDatabaseSession() { return null; }
            @Override public ICryptoStoreManager getCryptoStoreManager() { return csm; }
            @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
            @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
            @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
            @Override public BuildMetadata getBuildMetadata() { return BuildMetadata.builder().build(); }
            @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
        };

        X509CRL crl = CrlGenerator.generate(CrlGenerator.Request.builder()
                .revokedCertificates(Collections.emptyList())
                .caCert(interCert)
                .caPrivateKey(interKey.getPrivate())
                .updateMinutes(5)
                .build());
        CrlStore.entryMap.put("test", new CrlStore.CrlEntry(LocalTime.now(), crl));
    }

    @AfterEach
    void cleanup() {
        CrlStore.entryMap.clear();
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
        KeyPair intKey = csm.getIntermediateCerificateAuthorityKeyPair("test");
        X509Certificate intCert = csm.getX509CertificateForProvisioner("test");
        RevokedCertificate rc = new RevokedCertificate(BigInteger.TEN, new Date(), 0);
        X509CRL crl = CrlGenerator.generate(CrlGenerator.Request.builder()
                .revokedCertificate(rc)
                .caCert(intCert)
                .caPrivateKey(intKey.getPrivate())
                .updateMinutes(5)
                .build());
        CrlStore.entryMap.put("test", new CrlStore.CrlEntry(LocalTime.now(), crl));

        OCSPResp resp = OcspHelper.processOCSPRequest(BigInteger.TEN, prov, si);
        BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
        assertTrue(basic.getResponses()[0].getCertStatus() instanceof RevokedStatus);
    }
}
