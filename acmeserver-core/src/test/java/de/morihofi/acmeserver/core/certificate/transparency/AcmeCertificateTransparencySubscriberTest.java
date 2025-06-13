package de.morihofi.acmeserver.core.certificate.transparency;

import de.morihofi.acmeserver.ct.CertificateTransparencyClient;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.types.config.CertificateTransparencyConfig;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.events.AcmeCertificateCreatedEvent;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

class AcmeCertificateTransparencySubscriberTest {
    static class DummyServer implements IServerInstance {
        private final CryptoStoreManager mgr; private final EventBus bus; private final Config cfg;
        DummyServer(CryptoStoreManager mgr, EventBus bus, Config cfg){this.mgr=mgr;this.bus=bus;this.cfg=cfg;}
        @Override public String getServerURL(){return "";}@Override public org.hibernate.Session getDatabaseSession(){return null;}
        @Override public ICryptoStoreManager getCryptoStoreManager(){return mgr;}@Override public Config getAppConfig(){return cfg;}
        @Override public INonceManager getNonceManager(){return null;}@Override public RootCa getRootCa(){return null;}
        @Override public BuildMetadata getBuildMetadata(){return BuildMetadata.builder().build();}
        @Override public INetworkClient getNetworkClient(){return null;}@Override public EventBus getEventBus(){return bus;}
    }

    @BeforeAll
    static void setupProvider(){Security.addProvider(new BouncyCastleProvider());}

    private static CertificateConfig cfg(String cn){
        CertificateMetadata meta = new CertificateMetadata(cn, "Org", null, "DE");
        CertificateExpiration exp = new CertificateExpiration(0,0,1);
        return new CertificateConfig(meta, exp, null);
    }

    @Test
    @DisplayName("subscriber posts chain when enabled")
    void testSubmission() throws Exception {
        Path ksPath = Files.createTempDirectory("ks").resolve("store.p12");
        CryptoStoreManager mgr = new CryptoStoreManager(new PKCS12KeyStoreConfig(ksPath, "pw".toCharArray()));
        KeyStore ks = mgr.getKeyStore();
        KeyPair rootKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate rootCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("Root"))
                .ownKeyPair(rootKey)
                .build());
        KeyPair interKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate interCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.INTERMEDIATE_CA)
                .issuerKeyPair(rootKey)
                .issuerCertificate(rootCert)
                .ownKeyPair(interKey)
                .certificateConfig(cfg("Intermediate"))
                .build());
        ks.setKeyEntry("intermediateCA_test", interKey.getPrivate(), "".toCharArray(),
                new java.security.cert.Certificate[]{interCert, rootCert});

        KeyPair serverKey = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        Identifier id = new Identifier(Identifier.IDENTIFIER_TYPE.DNS, "example.com");
        Date start = new Date();
        Date end = new Date(start.getTime()+1000L);
        X509Certificate serverCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.SERVER)
                .issuerKeyPair(interKey)
                .issuerCertificate(interCert)
                .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                .identifier(id)
                .startDate(start)
                .endDate(end)
                .build());

        AcmeProvisioner prov = new AcmeProvisioner(); prov.setName("test");
        AcmeAccount acc = new AcmeAccount(); acc.setAcmeProvisioner(prov);
        AcmeOrder order = new AcmeOrder(); order.setAccount(acc);
        order.setCertificatePem(PemUtil.certificateToPEM(serverCert.getEncoded()));

        EventBus bus = new EventBus();
        Config cfg = new Config();
        CertificateTransparencyConfig ctCfg = new CertificateTransparencyConfig();
        ctCfg.setEnabled(true); ctCfg.setLogServer("http://ct"); ctCfg.setSubmitPreCertificate(false);
        cfg.setCertificateTransparency(ctCfg);
        IServerInstance si = new DummyServer(mgr,bus,cfg);

        CertificateTransparencyClient client = Mockito.mock(CertificateTransparencyClient.class);
        AcmeCertificateTransparencySubscriber sub = new AcmeCertificateTransparencySubscriber(si, client);
        bus.register(sub);

        bus.publish(new AcmeCertificateCreatedEvent(order, serverCert));
        Mockito.verify(client).submitChain(Mockito.anyList(), Mockito.eq(false));
    }
}
