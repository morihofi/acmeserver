package de.morihofi.acmeserver.core.api.acme.api.endpoints.order;
import de.morihofi.acmeserver.types.events.EventBus;

import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.cryptography.keystore.CryptoStoreManager;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.cryptography.keystore.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderCertEndpointTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    static class DummyServerInstance implements IServerInstance {
        private final CryptoStoreManager mgr;
        DummyServerInstance(CryptoStoreManager mgr) { this.mgr = mgr; }
        @NotNull
        @Override public String getServerURL() { return ""; }
        @NotNull
        @Override public Session getDatabaseSession() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.intf.ICryptoStoreManager getCryptoStoreManager() { return mgr; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.config.Config getAppConfig() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.intf.INonceManager getNonceManager() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.database.entities.RootCa getRootCa() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.runtime.BuildMetadata getBuildMetadata() { return null; }
        @NotNull
        @Override public de.morihofi.acmeserver.types.intf.network.INetworkClient getNetworkClient() { return null; }
            @NotNull
            @Override public EventBus getEventBus() { return new EventBus(); }
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = new CertificateMetadata(cn, "Org", null, "DE");
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }

    @Test
    @DisplayName("getCertificateChainOfACMEbyCertificateId returns leaf-first chain")
    void testCertificateChainOrder() throws Exception {
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
        Date end = new Date(start.getTime() + 1000L);
        X509Certificate serverCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.SERVER)
                .issuerKeyPair(interKey)
                .issuerCertificate(interCert)
                .serverPublicKeyBytes(serverKey.getPublic().getEncoded())
                .identifier(id)
                .startDate(start)
                .endDate(end)
                .build());

        AcmeOrder order = new AcmeOrder();
        order.setCertificatePem(PemUtil.certificateToPEM(serverCert.getEncoded()));
        AcmeProvisioner provisioner = new AcmeProvisioner();
        provisioner.setName("test");

        IServerInstance si = new DummyServerInstance(mgr);
        OrderCertEndpoint endpoint = new OrderCertEndpoint(si);

        Method m = OrderCertEndpoint.class.getDeclaredMethod(
                "getCertificateChainOfACMEbyCertificateId", AcmeOrder.class, AcmeProvisioner.class, IServerInstance.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<X509Certificate> chain = (List<X509Certificate>) m.invoke(endpoint, order, provisioner, si);

        assertEquals(List.of(serverCert, interCert, rootCert), chain);
    }
}
