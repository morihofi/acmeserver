package de.morihofi.acmeserver.core.web;

import com.google.common.jimfs.Jimfs;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.config.ServerConfig;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JettyCertificateHelperTest {

    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final RootCa rootCa;
        DummyServer(CryptoStoreManager mgr, RootCa rootCa){this.mgr=mgr;this.rootCa=rootCa;}
        @NotNull @Override public String getServerURL(){return "";}
        @NotNull @Override public org.hibernate.Session getDatabaseSession(){return null;}
        @NotNull @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}
        @NotNull @Override public Config getAppConfig(){Config c=new Config();c.setServer(new ServerConfig());return c;}
        @NotNull @Override public INonceManager getNonceManager(){return null;}
        @NotNull @Override public RootCa getRootCa(){return rootCa;}
        @NotNull @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @NotNull @Override public INetworkClient getNetworkClient(){return null;}
        @NotNull @Override public EventBus getEventBus(){return new EventBus();}
        @NotNull @Override public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags(){return Collections.emptySet();}
    }

    @BeforeAll
    static void addProvider(){
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("generate creates certificate when missing and returns null afterwards")
    void testGenerateAcmeApiClientCertificate() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path ksPath = fs.getPath("store.p12");
        CryptoStoreManager csm = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "pw".toCharArray()));

        CertificateMetadata meta = new CertificateMetadata("root", "", "", "");
        CertificateConfig cfg = new CertificateConfig(meta, new CertificateExpiration(0,0,1), new RsaCertificateAlgorithm(1024));
        RootCa root = new RootCa();
        root.setInternalUuid("root");
        root.setCertificateConfig(cfg);

        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg)
                .ownKeyPair(rootKey)
                .build());
        csm.getKeyStore().setKeyEntry(root.getInternalUuid(), rootKey.getPrivate(), "".toCharArray(), new java.security.cert.Certificate[]{rootCert});
        csm.saveKeystore();

        IServerInstance si = new DummyServer(csm, root);
        Config app = si.getAppConfig();
        app.getServer().setDnsName("example.com");

        CertificateRenewScheduler.CertificateData data = JettyCertificateHelper.generateAcmeApiClientCertificate(si);
        assertNotNull(data);

        csm.getKeyStore().setKeyEntry(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, data.keyPair().getPrivate(), "".toCharArray(), data.certificateChain());
        csm.saveKeystore();

        assertNull(JettyCertificateHelper.generateAcmeApiClientCertificate(si));
    }
}
