/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.certificate.helper;

import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CaInitHelper {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Initializes the Certificate Authority (CA) by generating or loading the CA certificate and key pair.
     *
     * @throws NoSuchAlgorithmException           If the specified algorithm is not available.
     * @throws CertificateException               If an issue occurs during certificate generation or loading.
     * @throws IOException                        If an I/O error occurs while creating directories or writing files.
     * @throws OperatorCreationException          If there's an issue with operator creation during certificate generation.
     * @throws NoSuchProviderException            If the specified security provider is not available.
     * @throws InvalidAlgorithmParameterException If there's an issue with algorithm parameters during key pair generation.
     */
    public static void initializeCA(CryptoStoreManager cryptoStoreManager, Config
            appConfig) throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException,
            NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {

        KeyStore caKeyStore = cryptoStoreManager.getKeyStore();
        if (!caKeyStore.containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)) {

            // Create CA

            KeyPair caKeyPair = null;
            if (appConfig.getRootCA().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                LOG.info("Using RSA algorithm");
                LOG.info("Generating RSA {} bit Key Pair for Root CA", rsaParams.getKeySize());
                caKeyPair = de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(),
                        caKeyStore.getProvider().getName());
            }
            if (appConfig.getRootCA().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                LOG.info("Using ECDSA algorithm (Elliptic curves");

                LOG.info("Generating ECDSA Key Pair using curve {} for Root CA", ecdsaAlgorithmParams.getCurveName());
                caKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName(), caKeyStore.getProvider().getName());
            }
            if (caKeyPair == null) {
                throw new IllegalArgumentException(
                        "Unknown algorithm " + appConfig.getRootCA().getAlgorithm() + " used for root certificate");
            }

            LOG.info("Creating CA");
            X509Certificate caCertificate =
                    CertificateAuthorityGenerator.generateCertificateAuthorityCertificate(appConfig.getRootCA(), caKeyPair);

            // Dumping CA Certificate to HDD, so other clients can install it
            LOG.info("Writing CA to keystore");
            caKeyStore.setKeyEntry(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA, caKeyPair.getPrivate(), "".toCharArray(), // No password
                    new X509Certificate[]{caCertificate});
            // Save CA in Keystore
            LOG.info("Saving keystore");
            cryptoStoreManager.saveKeystore();
        }
    }
}
