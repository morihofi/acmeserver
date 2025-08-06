package de.morihofi.certgine.acme;

import de.morihofi.certgine.acme.servlets.AcmeHttpServlet;
import de.morihofi.certgine.acme.servlets.GetHttpsForFreeServlet;
import de.morihofi.certgine.acme.tools.certificate.renew.watcher.ProvisionerRenewSubscriber;
import de.morihofi.certgine.acme.types.entities.*;
import de.morihofi.certgine.acme.types.events.ProvisionerCreatedEvent;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.database.entities.authority.*;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import de.morihofi.certgine.utils.scheduler.CertificateRenewScheduler;
import jakarta.servlet.http.HttpServlet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Certgine module providing ACME functionality.
 */
@Slf4j
@ModuleDescriptor(moduleName = "acme", description = "ACME API and related entities")
public class AcmeModule extends CertgineModule {

    public AcmeModule(IServerInstance serverInstance) {
        super(serverInstance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of(
                AcmeAccount.class,
                AcmeExternalAccountBinding.class,
                AcmeOrder.class,
                AcmeOrderIdentifier.class,
                AcmeOrderIdentifierChallenge.class,
                AcmeProvisioner.class,
                AcmeProvisionerDomainNameRestriction.class,
                AcmeHttpNonce.class,
                AcmeProvisionerMeta.class
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(
                AcmeHttpServlet.class,
                GetHttpsForFreeServlet.class
        );
    }

    @Override
    public void onModuleInitialize(IServerInstance serverInstance) {
        Optional<CertificateRenewScheduler> scheduler =
                serverInstance.getModuleRegistry().getService(CertificateRenewScheduler.class);
        if (scheduler.isPresent()) {
            ProvisionerRenewSubscriber sub = new ProvisionerRenewSubscriber(serverInstance, scheduler.get());
            serverInstance.getEventBus().register(sub);
            sub.initialize();
        } else {
            log.warn("CertificateRenewScheduler service not available; provisioner renew watchers disabled");
        }

        ensureDefaultProvisioner(serverInstance);
    }

    private void ensureDefaultProvisioner(IServerInstance serverInstance) {
        try (Session session = serverInstance.getDatabaseSession()) {
            if (!session.createQuery("FROM AcmeProvisioner", AcmeProvisioner.class).list().isEmpty()) {
                return;
            }
            Transaction tx = session.beginTransaction();

            int keySize = 4096;
            KeyPair intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(
                    keySize, serverInstance.getCryptoStoreManager().getKeyStoreProviderName());

            CertificateConfig intConfig = new CertificateConfig(
                    CertificateMetadata.builder().commonName("Certgine Default Intermediate").build(),
                    new CertificateExpiration(0, 0, 5),
                    new RsaCertificateAlgorithm(keySize)
            );

            RootCa rootCa = serverInstance.getRootCa();
            X509Certificate intermediateCert = X509Generator.generate(
                    X509Generator.Request.builder()
                            .type(X509Generator.Type.INTERMEDIATE_CA)
                            .certificateConfig(intConfig)
                            .ownKeyPair(intermediateKeyPair)
                            .issuerKeyPair(serverInstance.getCryptoStoreManager().getCertificateAuthorityKeyPair(rootCa))
                            .issuerCertificate(serverInstance.getCryptoStoreManager().getCertificateAuthorityX509Certificate(rootCa))
                            .build()
            );

            AcmeProvisioner provisioner = new AcmeProvisioner();
            provisioner.setInternalUuid(UUID.randomUUID().toString());

            IntermediateCa intermediateCa = new IntermediateCa();
            intermediateCa.setInternalUuid(provisioner.getInternalUuid());
            intermediateCa.setCertificateConfig(intConfig);
            provisioner.setName("default");
            provisioner.setRootCa(rootCa);
            provisioner.setMeta(new AcmeProvisionerMeta("", ""));
            provisioner.setIntermediateCa(intermediateCa);
            provisioner.setIssuedCertificateExpiration(new CertificateExpiration(0, 3, 0));
            provisioner.setWildcardAllowed(false);
            provisioner.setIpAllowed(true);
            AcmeProvisionerDomainNameRestriction restr = new AcmeProvisionerDomainNameRestriction();
            restr.setEnabled(false);
            restr.setMustEndWith(Collections.emptyList());
            provisioner.setAcmeProvisionerDomainNameRestriction(restr);

            serverInstance.getCryptoStoreManager().addIntermediateCertificateAuthority(
                    new X509Certificate[]{
                            intermediateCert,
                            serverInstance.getCryptoStoreManager().getCertificateAuthorityX509Certificate(rootCa)
                    },
                    intermediateKeyPair,
                    provisioner.getInternalUuid()
            );

            session.persist(provisioner);
            tx.commit();
            EventBus bus = serverInstance.getEventBus();
            bus.publish(new ProvisionerCreatedEvent(provisioner));
            log.info("Created default ACME provisioner");
        } catch (Exception e) {
            log.warn("Failed to create default ACME provisioner", e);
        }
    }

    @Override
    public @NonNull CertgineModuleInstance getModuleInstance() {
        return new AcmeModuleInstance(this);
    }
}
