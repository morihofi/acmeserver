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
     *
     * @param h                  the Hibernate utility providing the session factory
     * @param cryptoStoreManager the cryptographic store manager used for key operations
     * @param eventBus           the event bus for notification handling
     * @return the root certificate authority entity
     * @throws NoSuchAlgorithmException  if a required algorithm is missing
     * @throws CertificateException      if certificate creation fails
     * @throws IOException               if a keystore write fails
     * @throws OperatorCreationException if the operator cannot be created
     * @throws NoSuchProviderException   if the provider is missing
     * @throws KeyStoreException         if the keystore cannot be accessed
     * @throws UnrecoverableKeyException if the key cannot be recovered
     */
    public static RootCa initializeCA(@NonNull HibernateUtil h, ICryptoStoreManager cryptoStoreManager, EventBus eventBus)
            throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException,
            NoSuchProviderException, KeyStoreException, UnrecoverableKeyException {
        try (Session session = h.getSessionFactory().openSession()) {
            RootCa rootCaEntity = createRootCaIfMissing(session, cryptoStoreManager);
            if (rootCaEntity == null) {
                rootCaEntity = loadExistingRootCa(session, cryptoStoreManager);
            }
            return rootCaEntity;
        }
    }

    /**
     * Creates a new root CA if none exist yet.
     *
     * @param session            the active Hibernate session
     * @param cryptoStoreManager the cryptographic store manager used for key operations
     * @return the newly created root CA or {@code null} if a root CA already exists
     * @throws NoSuchAlgorithmException  if a required algorithm is missing
     * @throws CertificateException      if certificate creation fails
     * @throws IOException               if a keystore write fails
     * @throws OperatorCreationException if the operator cannot be created
     * @throws NoSuchProviderException   if the provider is missing
     * @throws KeyStoreException         if the keystore cannot be accessed
     */
    private static RootCa createRootCaIfMissing(Session session, ICryptoStoreManager cryptoStoreManager)
            throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException,
            NoSuchProviderException, KeyStoreException {
        if (RootCa.getAllRoots(session).length != 0) {
            return null;
        }
        Transaction transaction = session.beginTransaction();

        final int keySize = 4096;

        log.info("Using RSA algorithm");
        log.info("Generating new RSA {} bit Key Pair for Root CA", keySize);
        KeyPair caKeyPair = KeyPairGenerator.generateRSAKeyPair(keySize, cryptoStoreManager.getKeyStoreProviderName());

        RootCa rootCaEntity = new RootCa();
        rootCaEntity.setCertificateConfig(new CertificateConfig(
                CertificateMetadata.builder()
                        .commonName("Certgine Default Root CA")
                        .build(),
                new CertificateExpiration(0, 0, 20),
                new RsaCertificateAlgorithm(keySize)
        ));
        rootCaEntity.setInternalUuid(UUID.randomUUID().toString());

        log.info("Creating CA");
        X509Certificate caCertificate = X509Generator.generate(
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
        return rootCaEntity;
    }

    /**
     * Loads an existing root CA from the database and ensures its key pair and certificate are available.
     *
     * @param session            the active Hibernate session
     * @param cryptoStoreManager the cryptographic store manager used for key operations
     * @return the selected root CA
     * @throws UnrecoverableKeyException if the key cannot be recovered
     * @throws KeyStoreException         if the keystore cannot be accessed
     * @throws NoSuchAlgorithmException  if a required algorithm is missing
     */
    private static RootCa loadExistingRootCa(Session session, ICryptoStoreManager cryptoStoreManager)
            throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        RootCa[] roots = RootCa.getAllRoots(session);
        RootCa rootCaEntity = selectRootCa(roots);
        cryptoStoreManager.getCertificateAuthorityKeyPair(rootCaEntity);
        cryptoStoreManager.getCertificateAuthorityX509Certificate(rootCaEntity);
        return rootCaEntity;
    }

    /**
     * Selects the root CA to use. Currently picks the first available root CA.
     *
     * @param roots the array of available root CAs
     * @return the chosen root CA
     */
    private static RootCa selectRootCa(RootCa[] roots) {
        if (roots.length == 0) {
            throw new IllegalStateException("No root CA available");
        }
        RootCa rootCaEntity = roots[0];
        if (roots.length > 1) {
            log.warn("Multiple root CAs found; using the first one with UUID {}", rootCaEntity.getInternalUuid());
            // TODO: Implement proper selection of root CA when multiple exist (see issue #123)
        }
        return rootCaEntity;
    }
}
