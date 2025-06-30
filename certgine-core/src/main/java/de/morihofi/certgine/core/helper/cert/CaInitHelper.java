/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.helper.cert;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisionerDomainNameRestriction;
import de.morihofi.certgine.types.database.entities.acme.ProvisionerMeta;
import de.morihofi.certgine.types.database.entities.authority.*;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.events.ProvisionerCreatedEvent;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

/**
 * Helper class for initializing the Certificate Authority (CA).
 */
@Slf4j
public class CaInitHelper {

    /**
     * Initializes the Certificate Authority (CA) by generating or loading the CA certificate and key pair.
     */
    public static RootCa initializeCA(@NonNull HibernateUtil h, ICryptoStoreManager cryptoStoreManager, EventBus eventBus) throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException,
            NoSuchProviderException, KeyStoreException, UnrecoverableKeyException {
        try (Session session = h.getSessionFactory().openSession()) {

            RootCa rootCaEntity;
            KeyPair caKeyPair;
            X509Certificate caCertificate;

            if (RootCa.getAllRoots(session).length == 0) {
                Transaction transaction = session.beginTransaction();

                final int keySize = 4096;

                log.info("Using RSA algorithm");
                log.info("Generating new RSA {} bit Key Pair for Root CA", keySize);
                caKeyPair = KeyPairGenerator.generateRSAKeyPair(keySize, cryptoStoreManager.getKeyStoreProviderName());

                rootCaEntity = new RootCa();
                rootCaEntity.setCertificateConfig(new CertificateConfig(
                        CertificateMetadata.builder()
                                .commonName("Certgine Default Root CA")
                                .build(),
                        new CertificateExpiration(0, 0, 20),
                        new RsaCertificateAlgorithm(keySize)
                ));
                rootCaEntity.setInternalUuid(UUID.randomUUID().toString());

                log.info("Creating CA");
                caCertificate = X509Generator.generate(
                        X509Generator.Request.builder()
                                .type(X509Generator.Type.ROOT_CA)
                                .certificateConfig(rootCaEntity.getCertificateConfig())
                                .ownKeyPair(caKeyPair)
                                .build()
                );

                log.info("Writing CA to keystore");
                cryptoStoreManager.addCertificateAuthority(rootCaEntity, caKeyPair, caCertificate);


                log.info("Persisting root CA in database");
                session.persist(rootCaEntity);
                transaction.commit();
            } else {
                rootCaEntity = RootCa.getAllRoots(session)[0]; //FIXME: Return correct one ... somehow
                caKeyPair = cryptoStoreManager.getCertificateAuthorityKeyPair(rootCaEntity);
                caCertificate = cryptoStoreManager.getCertificateAuthorityX509Certificate(rootCaEntity);
            }

            Transaction transaction = session.beginTransaction();
            if (session.createQuery("FROM AcmeProvisioner", AcmeProvisioner.class).list().isEmpty()) {
                createDefaultProvisioner(session, cryptoStoreManager, rootCaEntity, caKeyPair, caCertificate, eventBus);
            }
            transaction.commit();

            return rootCaEntity;

        }
    }

    private static void createDefaultProvisioner(Session session, ICryptoStoreManager cryptoStoreManager,
                                                 RootCa rootCa, KeyPair caKeyPair, X509Certificate caCertificate,
                                                 EventBus eventBus)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, OperatorCreationException, IOException, NoSuchProviderException {

        log.info("Creating default provisioner");

        final int keySize = 4096;
        KeyPair intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(keySize, cryptoStoreManager.getKeyStoreProviderName());

        CertificateConfig intConfig = new CertificateConfig(
                CertificateMetadata.builder()
                        .commonName("CertgineDefault Intermediate")
                        .build(),
                new CertificateExpiration(0, 0, 5),
                new RsaCertificateAlgorithm(keySize)
        );

        X509Certificate intermediateCert = de.morihofi.certgine.cryptography.certificate.X509Generator.generate(
                de.morihofi.certgine.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.certgine.cryptography.certificate.X509Generator.Type.INTERMEDIATE_CA)
                        .certificateConfig(intConfig)
                        .ownKeyPair(intermediateKeyPair)
                        .issuerKeyPair(caKeyPair)
                        .issuerCertificate(caCertificate)
                        .build()
        );

        AcmeProvisioner provisioner = new AcmeProvisioner();
        provisioner.setInternalUuid(UUID.randomUUID().toString());

        IntermediateCa intermediateCa = new IntermediateCa();
        intermediateCa.setInternalUuid(provisioner.getInternalUuid());
        intermediateCa.setCertificateConfig(intConfig);
        provisioner.setName("default");
        provisioner.setRootCa(rootCa);
        provisioner.setMeta(new ProvisionerMeta("", ""));
        provisioner.setIntermediateCa(intermediateCa);
        provisioner.setIssuedCertificateExpiration(new CertificateExpiration(0, 3, 0));
        provisioner.setWildcardAllowed(false);
        provisioner.setIpAllowed(true);
        AcmeProvisionerDomainNameRestriction restr = new AcmeProvisionerDomainNameRestriction();
        restr.setEnabled(false);
        restr.setMustEndWith(java.util.Collections.emptyList());
        provisioner.setAcmeProvisionerDomainNameRestriction(restr);


        cryptoStoreManager.addIntermediateCertificateAuthority(
                new X509Certificate[]{
                        intermediateCert,
                        caCertificate
                },
                intermediateKeyPair,
                provisioner.getInternalUuid()
        );

        session.persist(provisioner);
        eventBus.publish(new ProvisionerCreatedEvent(provisioner));
    }
}
