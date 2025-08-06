package de.morihofi.certgine.core.web;

import com.google.common.jimfs.Jimfs;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.core.modules.CoreModule;
import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.core.tools.certificate.renew.watcher.CertificateRenewScheduler;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.ServerConfig;
import de.morihofi.certgine.types.database.entities.authority.*;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.INonceManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import lombok.NonNull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.eclipse.jetty.server.Server;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class TlsCertificateManagerTest {

    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final RootCa rootCa;
        private final Config config;
        private final EventBus eventBus;
        private final ModuleRegistry moduleRegistry;

        DummyServer(CryptoStoreManager mgr, RootCa rootCa, Config config, EventBus eventBus,
                    ModuleRegistry moduleRegistry) {
            this.mgr = mgr;
            this.rootCa = rootCa;
            this.config = config;
            this.eventBus = eventBus;
            this.moduleRegistry = moduleRegistry;
        }

        @NotNull
        @Override
        public String getServerURL() {
            return "";
        }

        @NotNull
        @Override
        public org.hibernate.Session getDatabaseSession() {
            return null;
        }

        @NotNull
        @Override
        public ICryptoStoreManager getCryptoStoreManager() {
            return mgr;
        }

        @NotNull
        @Override
        public Config getAppConfig() {
            return config;
        }

        @NotNull
        @Override
        public INonceManager getNonceManager() {
            return null;
        }

        @NotNull
        @Override
        public RootCa getRootCa() {
            return rootCa;
        }

        @NotNull
        @Override
        public BuildMetadata getBuildMetadata() {
            return BuildMetadata.builder().build();
        }

        @NotNull
        @Override
        public INetworkClient getNetworkClient() {
            return null;
        }

        @NotNull
        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @NotNull
        @Override
        public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags() {
            return java.util.Collections.emptySet();
        }

        @Override
        public @NonNull IModuleRegistry getModuleRegistry() {
            return moduleRegistry;
        }
    }

    @BeforeAll
    static void addProviders() {
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastleJsseProvider());
    }

    private DummyServer createServer(CryptoStoreManager csm, RootCa root) {
        Config cfg = new Config();
        cfg.setServer(new ServerConfig());
        EventBus bus = new EventBus();
        ModuleRegistry registry = new ModuleRegistry();
        CoreModule core = new CoreModule();
        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("core")
                .module(core)
                .build());
        DummyServer server = new DummyServer(csm, root, cfg, bus, registry);
        core.onModuleInitialize(server);
        return server;
    }

    private RootCa createRootCa(CryptoStoreManager csm) throws Exception {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName("root")
                .build();
        CertificateConfig config = new CertificateConfig(meta,
                new CertificateExpiration(0, 0, 1),
                new RsaCertificateAlgorithm(1024));
        RootCa root = new RootCa();
        root.setInternalUuid("root");
        root.setCertificateConfig(config);
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(1024, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(config)
                .ownKeyPair(rootKey)
                .build());
        csm.addCertificateAuthority(root, rootKey, rootCert);
        return root;
    }

    @Test
    @DisplayName("scheduler is initialized and watcher registered")
    void schedulerInitialized() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path ksPath = fs.getPath("store.p12");
        CryptoStoreManager csm = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "pw".toCharArray()));
        RootCa root = createRootCa(csm);
        DummyServer server = createServer(csm, root);
        Server jetty = new Server();

        TlsCertificateManager mgr = new TlsCertificateManager(server, jetty);
        CertificateRenewScheduler sched =
                server.getModuleRegistry().getService(CertificateRenewScheduler.class);
        assertNotNull(sched);
        assertTrue(sched.isWatcherRegistered(CryptoStoreManager.KEYSTORE_ALIASPREFIX_SERVER));
    }

    @Test
    @DisplayName("setupTls generates certificate and initializes connector")
    void setupTlsWorks() throws Exception {
        FileSystem fs = Jimfs.newFileSystem();
        Path ksPath = fs.getPath("store.p12");
        CryptoStoreManager csm = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "pw".toCharArray()));
        RootCa root = createRootCa(csm);
        DummyServer server = createServer(csm, root);
        Server jetty = new Server();

        TlsCertificateManager mgr = new TlsCertificateManager(server, jetty);
        mgr.setupTls();
        assertTrue(csm.containsServerCertificate("main"));
        assertEquals(1, jetty.getConnectors().length);
    }
}
