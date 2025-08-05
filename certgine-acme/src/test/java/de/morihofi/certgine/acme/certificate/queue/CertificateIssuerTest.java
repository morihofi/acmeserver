package de.morihofi.certgine.acme.certificate.queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.morihofi.certgine.acme.types.entities.*;
import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.config.ServerConfig;
import de.morihofi.certgine.types.config.network.NetworkConfig;
import de.morihofi.certgine.acme.types.entities.enums.AcmeOrderState;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.types.database.entities.authority.CertificateConfig;
import de.morihofi.certgine.types.database.entities.authority.CertificateExpiration;
import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import de.morihofi.certgine.acme.types.events.AcmeCertificateCreatedEvent;
import de.morihofi.certgine.acme.types.events.BeforeAcmeCertificateCreatedEvent;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.exception.exceptions.ACMECaaException;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.utils.base64.Base64Tools;
import de.morihofi.certgine.utils.network.dns.CAAValidator;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;

class CertificateIssuerTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static String createCsr(String domain) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        X500Name subject = new X500Name("CN=" + domain);
        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        GeneralName[] names = new GeneralName[]{new GeneralName(GeneralName.dNSName, domain)};
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
        builder.addAttribute(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        PKCS10CertificationRequest csr = builder.build(signer);
        return Base64Tools.base64UrlEncode(csr.getEncoded());
    }

    private static AcmeOrder prepareOrder(String csr, AcmeProvisioner provisioner) {
        AcmeOrder order = new AcmeOrder();
        order.setOrderId("order1");
        order.setCertificateCSR(csr);
        order.setNotBefore(Instant.now());
        order.setNotAfter(Instant.now().plusSeconds(3600));
        order.setOrderState(AcmeOrderState.NEED_A_CERTIFICATE);

        AcmeAccount account = new AcmeAccount();
        account.setAccountId("acc1");
        account.setAcmeProvisioner(provisioner);
        order.setAccount(account);

        AcmeOrderIdentifier dns = new AcmeOrderIdentifier("dns", "example.com");
        AcmeOrderIdentifierChallenge ch = new AcmeOrderIdentifierChallenge(null, dns, "cid1", "tok");
        ch.setStatus(AcmeStatus.VALID);
        dns.setChallenges(List.of(ch));
        order.setOrderIdentifiers(List.of(dns));
        return order;
    }

    private static AcmeProvisioner createProvisioner() {
        AcmeProvisioner provisioner = new AcmeProvisioner();
        provisioner.setName("prov1");
        provisioner.setInternalUuid("uuid1");
        return provisioner;
    }

    @Test
    @DisplayName("disallowed CAA record triggers ACMECaaException")
    void disallowedCaaThrows() throws Exception {
        AcmeProvisioner provisioner = createProvisioner();
        String csr = createCsr("example.com");
        AcmeOrder order = prepareOrder(csr, provisioner);

        ICryptoStoreManager csm = mock(ICryptoStoreManager.class);
        Session session = mock(Session.class);
        IServerInstance server = mock(IServerInstance.class);
        EventBus bus = mock(EventBus.class);
        when(server.getEventBus()).thenReturn(bus);
        Config cfg = new Config();
        cfg.setServer(new ServerConfig());
        cfg.setNetwork(new NetworkConfig());
        when(server.getAppConfig()).thenReturn(cfg);
        when(server.getNetworkClient()).thenReturn(mock(INetworkClient.class));

        try (MockedStatic<CAAValidator> mockCaa = mockStatic(CAAValidator.class)) {
            mockCaa.when(() -> CAAValidator.isIssuanceAllowed(any(), any(), any(), any())).thenReturn(false);
            assertThrows(ACMECaaException.class,
                    () -> CertificateIssuer.generateCertificateForOrder(order, csm, session, server));
        }
    }

    @Test
    @DisplayName("successful issuance updates order and publishes events")
    void successfulIssuanceUpdatesOrderAndPublishesEvents() throws Exception {
        AcmeProvisioner provisioner = createProvisioner();
        String csr = createCsr("example.com");
        AcmeOrder order = prepareOrder(csr, provisioner);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(512);
        KeyPair caKp = kpg.generateKeyPair();
        CertificateConfig caCfg = new CertificateConfig(
                CertificateMetadata.builder().commonName("Test CA").build(),
                new CertificateExpiration(0, 0, 1),
                null);
        X509Certificate caCert = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.ROOT_CA)
                        .certificateConfig(caCfg)
                        .ownKeyPair(caKp)
                        .build());

        ICryptoStoreManager csm = mock(ICryptoStoreManager.class);
        when(csm.getIntermediateCertificateAuthorityKeyPair(provisioner.getInternalUuid())).thenReturn(caKp);
        when(csm.getIntermediateCertificate(provisioner.getInternalUuid())).thenReturn(caCert);

        Session session = mock(Session.class);
        Transaction tx = mock(Transaction.class);
        when(session.beginTransaction()).thenReturn(tx);

        IServerInstance server = mock(IServerInstance.class);
        EventBus bus = mock(EventBus.class);
        when(server.getEventBus()).thenReturn(bus);
        Config cfg = new Config();
        cfg.setServer(new ServerConfig());
        cfg.getServer().setDnsName("ca.example.com");
        cfg.setNetwork(new NetworkConfig());
        when(server.getAppConfig()).thenReturn(cfg);
        when(server.getNetworkClient()).thenReturn(mock(INetworkClient.class));
        when(server.getServerURL()).thenReturn("https://ca.example.com");

        try (MockedStatic<CAAValidator> mockCaa = mockStatic(CAAValidator.class)) {
            mockCaa.when(() -> CAAValidator.isIssuanceAllowed(any(), any(), any(), any())).thenReturn(true);
            CertificateIssuer.generateCertificateForOrder(order, csm, session, server);
        }

        assertNotNull(order.getCertificateSerialNumber());
        assertNotNull(order.getCertificatePem());
        assertNotNull(order.getCertificateIssued());
        assertNotNull(order.getCertificateExpires());
        assertEquals(AcmeOrderState.IDLE, order.getOrderState());
        assertEquals(order.getCertificateExpires(), order.getExpires());

        verify(bus).publish(any(BeforeAcmeCertificateCreatedEvent.class));
        verify(bus).publish(any(AcmeCertificateCreatedEvent.class));
        verify(session).merge(order);
        verify(tx).commit();
    }
}
