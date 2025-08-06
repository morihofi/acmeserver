/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.web;

import com.google.common.jimfs.Jimfs;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keystore.CryptoStoreManager;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.utils.scheduler.CertificateRenewScheduler;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.ServerConfig;
import de.morihofi.certgine.types.database.entities.authority.*;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.runtime.BuildMetadata;
import de.morihofi.certgine.types.cryptography.keystore.PKCS12KeyStoreConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JettyCertificateHelperTest {

    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr;
        private final RootCa rootCa;
        DummyServer(CryptoStoreManager mgr, RootCa rootCa){this.mgr=mgr;this.rootCa=rootCa;}
        @NotNull
        @NonNull @Override public String getServerURL(){return "";}
        @NotNull
        @NonNull @Override public org.hibernate.Session getDatabaseSession(){return null;}
        @NotNull
        @NonNull @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}
        @NotNull
        @NonNull @Override public Config getAppConfig(){Config c=new Config();c.setServer(new ServerConfig());return c;}
        @NotNull
        @NonNull @Override public RootCa getRootCa(){return rootCa;}
        @NotNull
        @NonNull @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @NotNull
        @NonNull @Override public INetworkClient getNetworkClient(){return null;}
        @NotNull
        @NonNull @Override public EventBus getEventBus(){return new EventBus();}
        @NotNull
        @NonNull @Override public java.util.Set<de.morihofi.certgine.types.server.StartupFlag> getStartupFlags(){return Collections.emptySet();}

        @Override
        public @NonNull IModuleRegistry getModuleRegistry() {
            return null;
        }
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

        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName("root")
                .build();
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
        csm.addCertificateAuthority(root, rootKey, rootCert);

        IServerInstance si = new DummyServer(csm, root);
        Config app = si.getAppConfig();
        app.getServer().setDnsName("example.com");

        CertificateRenewScheduler.CertificateData data =
                JettyCertificateHelper.generateAcmeApiClientCertificate(si, Clock.systemUTC());
        assertNotNull(data);

        csm.addServerCertificate(data.certificateChain(), data.keyPair(), "main");

        assertTrue(csm.containsServerCertificate("main"));
        assertNull(JettyCertificateHelper.generateAcmeApiClientCertificate(si, Clock.systemUTC()));
    }
}
