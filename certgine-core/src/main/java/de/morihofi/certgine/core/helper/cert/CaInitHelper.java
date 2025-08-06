/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.helper.cert;

import de.morihofi.certgine.core.database.HibernateUtil;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.cryptography.keys.KeyPairGenerator;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.database.entities.authority.*;
import de.morihofi.certgine.types.events.EventBus;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.hibernate.Session;
import org.hibernate.Transaction;

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

            return rootCaEntity;

        }
    }
}
