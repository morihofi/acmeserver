/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.helper.cert;

import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.cryptography.keys.KeyPairGenerator;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.events.ProvisionerCreatedEvent;

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

                KeyStore caKeyStore = cryptoStoreManager.getKeyStore();
                final int keySize = 4096;

                log.info("Using RSA algorithm");
                log.info("Generating new RSA {} bit Key Pair for Root CA", keySize);
                caKeyPair = KeyPairGenerator.generateRSAKeyPair(keySize, caKeyStore.getProvider().getName());

                rootCaEntity = new RootCa();
                rootCaEntity.setCertificateConfig(new CertificateConfig(
                        new CertificateMetadata(
                                "ACME Server Default Root CA",
                                "",
                                "",
                                ""),
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
                caKeyStore.setKeyEntry(rootCaEntity.getInternalUuid(), caKeyPair.getPrivate(), "".toCharArray(), new X509Certificate[]{caCertificate});
                cryptoStoreManager.saveKeystore();

                log.info("Persisting root CA in database");
                session.persist(rootCaEntity);
                transaction.commit();
            } else {
                rootCaEntity = RootCa.getAllRoots(session)[0]; //FIXME: Return correct one ... somehow
                caKeyPair = cryptoStoreManager.getCerificateAuthorityKeyPair(rootCaEntity);
                caCertificate = cryptoStoreManager.getCerificateAuthorityX509Certificate(rootCaEntity);
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
        KeyPair intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(keySize, cryptoStoreManager.getKeyStore().getProvider().getName());

        CertificateConfig intConfig = new CertificateConfig(
                new CertificateMetadata(
                        "ACME Server Default Intermediate",
                        "",
                        "",
                        ""),
                new CertificateExpiration(0, 0, 5),
                new RsaCertificateAlgorithm(keySize)
        );

        X509Certificate intermediateCert = de.morihofi.acmeserver.cryptography.certificate.X509Generator.generate(
                de.morihofi.acmeserver.cryptography.certificate.X509Generator.Request.builder()
                        .type(de.morihofi.acmeserver.cryptography.certificate.X509Generator.Type.INTERMEDIATE_CA)
                        .certificateConfig(intConfig)
                        .ownKeyPair(intermediateKeyPair)
                        .issuerKeyPair(caKeyPair)
                        .issuerCertificate(caCertificate)
                        .build()
        );

        String alias = cryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate("default");
        cryptoStoreManager.getKeyStore().setKeyEntry(alias, intermediateKeyPair.getPrivate(), "".toCharArray(), new X509Certificate[]{intermediateCert, caCertificate});
        cryptoStoreManager.saveKeystore();

        AcmeProvisioner provisioner = new AcmeProvisioner();
        provisioner.setName("default");
        provisioner.setRootCa(rootCa);
        provisioner.setMeta(new ProvisionerMeta("", ""));
        provisioner.setCertificateConfig(intConfig);
        provisioner.setIssuedCertificateExpiration(new CertificateExpiration(0, 3, 0));
        provisioner.setWildcardAllowed(false);
        provisioner.setIpAllowed(true);
        AcmeProvisionerDomainNameRestriction restr = new AcmeProvisionerDomainNameRestriction();
        restr.setEnabled(false);
        restr.setMustEndWith(java.util.Collections.emptyList());
        provisioner.setAcmeProvisionerDomainNameRestriction(restr);

        session.persist(provisioner);
        eventBus.publish(new ProvisionerCreatedEvent(provisioner));
    }
}
