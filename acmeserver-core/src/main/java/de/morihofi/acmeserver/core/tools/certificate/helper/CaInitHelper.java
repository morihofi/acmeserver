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

package de.morihofi.acmeserver.core.tools.certificate.helper;

import de.morihofi.acmeserver.core.database.HibernateUtil;
import de.morihofi.acmeserver.core.database.objects.*;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import de.morihofi.acmeserver.core.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.core.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.core.tools.certificate.generator.KeyPairGenerator;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.hibernate.Session;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
    public static RootCa initializeCA(HibernateUtil hibernateUtil, CryptoStoreManager cryptoStoreManager) throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException,
            NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {

        if (RootCa.getAllRoots(hibernateUtil).length != 0) {
            return RootCa.getAllRoots(hibernateUtil)[0]; //FIXME: Return correct one ... somehow
            // Skip, because we already have at least one CA
        }

        // No CA is existing at the moment -> we need a new one

        KeyStore caKeyStore = cryptoStoreManager.getKeyStore();

        // Create CA
        final int keySize = 4096;


        try (Session session = hibernateUtil.getSessionFactory().openSession()) {

            log.info("Using RSA algorithm");
            log.info("Generating new RSA {} bit Key Pair for Root CA", keySize);
            KeyPair caKeyPair = KeyPairGenerator.generateRSAKeyPair(keySize, caKeyStore.getProvider().getName());

            RootCa rootCaEntity = new RootCa(); //TODO: Add Option for ENV Variables to be set on first run
            rootCaEntity.setCertificateConfig(new CertificateConfig(
                    new CertificateMetadata(
                            "ACME Server Default Root CA",
                            "",
                            "",
                            ""),
                    new CertificateExpiration(20,0,0),
                    new CertificateAlgorithm("rsa", keySize)
            ));
            rootCaEntity.setInternalUuid(UUID.randomUUID().toString());

            log.info("Creating CA");
            X509Certificate caCertificate =
                    CertificateAuthorityGenerator.generateCertificateAuthorityCertificate(rootCaEntity.getCertificateConfig(), caKeyPair);

            log.info("Writing CA to keystore");
            caKeyStore.setKeyEntry(rootCaEntity.getInternalUuid(), caKeyPair.getPrivate(), "".toCharArray(), // No password
                    new X509Certificate[]{caCertificate});

            log.info("Saving keystore");
            cryptoStoreManager.saveKeystore();

            log.info("Persisting root CA in database");
            session.persist(rootCaEntity);

            return rootCaEntity;

        }
    }
}
