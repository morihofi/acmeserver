package de.morihofi.acmeserver.cryptography.ocsp;

import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.types.config.Config;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.authority.CertificateConfig;
import de.morihofi.acmeserver.types.database.entities.authority.CertificateExpiration;
import de.morihofi.acmeserver.types.database.entities.authority.CertificateMetadata;
import de.morihofi.acmeserver.types.database.entities.authority.RootCa;
import de.morihofi.acmeserver.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.INonceManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.intf.network.INetworkClient;
import de.morihofi.acmeserver.types.runtime.BuildMetadata;
import lombok.NonNull;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.hibernate.Session;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class OcspProcessorTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static CertificateConfig cfg(String cn) {
        CertificateMetadata meta = CertificateMetadata.builder()
                .commonName(cn)
                .organisation("Org")
                .countryCode("DE")
                .build();
        CertificateExpiration exp = new CertificateExpiration(0, 0, 1);
        return new CertificateConfig(meta, exp, null);
    }

    static class DummyServer implements IServerInstance {
        private final ICryptoStoreManager csm;

        DummyServer() {
            this(Mockito.mock(ICryptoStoreManager.class));
        }

        DummyServer(ICryptoStoreManager csm) {
            this.csm = csm;
        }
        @NotNull
        @NonNull
        @Override
        public String getServerURL() {
            return "";
        }

        @NotNull
        @NonNull
        @Override
        public Session getDatabaseSession() {
            return null;
        }

        @NotNull
        @NonNull
        @Override
        public ICryptoStoreManager getCryptoStoreManager() {
            return csm;
        }

        @NotNull
        @NonNull
        @Override
        public Config getAppConfig() {
            return new Config();
        }

        @NotNull
        @NonNull
        @Override
        public INonceManager getNonceManager() {
            return null;
        }

        @NotNull
        @NonNull
        @Override
        public RootCa getRootCa() {
            return null;
        }

        @NotNull
        @Override
        public @NonNull TsaAuthority getTsaAuthority() {
            return null;
        }

        @NotNull
        @NonNull
        @Override
        public BuildMetadata getBuildMetadata() {
            return BuildMetadata.builder().build();
        }

        @NotNull
        @NonNull
        @Override
        public INetworkClient getNetworkClient() {
            return null;
        }

        @NotNull
        @NonNull
        @Override
        public EventBus getEventBus() {
            return new EventBus();
        }

        @NotNull
        @NonNull
        @Override
        public java.util.Set<de.morihofi.acmeserver.types.server.StartupFlag> getStartupFlags() {
            return java.util.Collections.emptySet();
        }
    }

    static class DummyProvisioner extends AcmeProvisioner {
        private final KeyPair kp;
        private final X509Certificate cert;

        DummyProvisioner(String name, KeyPair kp, X509Certificate cert) {
            this.kp = kp;
            this.cert = cert;
            setName(name);
        }
    }

    @Test
    @DisplayName("processOCSPRequest returns revoked status")
    void testProcessRevoked() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate caCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("CA"))
                .ownKeyPair(kp)
                .build());
        DummyProvisioner prov = new DummyProvisioner("p", kp, caCert);
        ICryptoStoreManager csm = Mockito.mock(ICryptoStoreManager.class);
        Mockito.when(csm.getIntermediateCertificate(prov.getInternalUuid())).thenReturn(caCert);
        Mockito.when(csm.getIntermediateCertificateAuthorityKeyPair(prov.getInternalUuid()))
                .thenReturn(kp);
        IServerInstance si = new DummyServer(csm);
        BigInteger serial = BigInteger.ONE;
        RevokedCertificate rc = new RevokedCertificate(serial, new Date(), 0);
        try (MockedStatic<AcmeOrder> mock = Mockito.mockStatic(AcmeOrder.class)) {
            mock.when(() -> AcmeOrder.getRevokedCertificate(serial, prov.getName(), si)).thenReturn(rc);
            OCSPResp resp = OcspProcessor.processOCSPRequest(serial, prov, si);
            assertEquals(OCSPRespBuilder.SUCCESSFUL, resp.getStatus());
            BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
            SingleResp sr = basic.getResponses()[0];
            assertTrue(sr.getCertStatus() instanceof RevokedStatus);
        }
    }

    @Test
    @DisplayName("processOCSPRequest returns good status when not revoked")
    void testProcessGood() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(512, BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate caCert = X509Generator.generate(X509Generator.Request.builder()
                .type(X509Generator.Type.ROOT_CA)
                .certificateConfig(cfg("CA"))
                .ownKeyPair(kp)
                .build());
        DummyProvisioner prov = new DummyProvisioner("p", kp, caCert);
        ICryptoStoreManager csm = Mockito.mock(ICryptoStoreManager.class);
        Mockito.when(csm.getIntermediateCertificate(prov.getInternalUuid())).thenReturn(caCert);
        Mockito.when(csm.getIntermediateCertificateAuthorityKeyPair(prov.getInternalUuid()))
                .thenReturn(kp);
        IServerInstance si = new DummyServer(csm);
        BigInteger serial = BigInteger.TWO;
        try (MockedStatic<AcmeOrder> mock = Mockito.mockStatic(AcmeOrder.class)) {
            mock.when(() -> AcmeOrder.getRevokedCertificate(serial, prov.getName(), si)).thenReturn(null);
            OCSPResp resp = OcspProcessor.processOCSPRequest(serial, prov, si);
            BasicOCSPResp basic = (BasicOCSPResp) resp.getResponseObject();
            SingleResp sr = basic.getResponses()[0];
            assertEquals(CertificateStatus.GOOD, sr.getCertStatus());
        }
    }
}
