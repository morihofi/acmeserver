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

package de.morihofi.acmeserver;

import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.config.keyStoreHelpers.PKCS12KeyStoreParams;
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.X509;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A utility class for migrating keys and certificates into a KeyStore.
 *
 * <p>
 * The `KSMigrationTool` class provides methods to migrate keys and certificates into a KeyStore. It is used during the migration process to
 * import the root CA certificate, intermediate CA certificates, and provisioner certificates into the KeyStore.
 * </p>
 */
public class KSMigrationTool {
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Runs the migration process to import certificates into the KeyStore.
     *
     * @param args               Command-line arguments.
     * @param cryptoStoreManager The CryptoStoreManager to manage the KeyStore.
     * @param appConfig          The configuration of the application.
     * @param filesDir           The directory containing certificate and key files.
     * @throws IOException              If an I/O error occurs.
     * @throws CertificateException     If a certificate error occurs.
     * @throws KeyStoreException        If a KeyStore error occurs.
     * @throws NoSuchAlgorithmException If a required algorithm is not available.
     */
    public static void run(String[] args, CryptoStoreManager cryptoStoreManager, Config appConfig, Path filesDir) throws IOException,
            CertificateException, KeyStoreException, NoSuchAlgorithmException {
        LOG.info("Starting in migration mode");
        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        if (appConfig.getKeyStore() instanceof PKCS12KeyStoreParams pkcs12KeyStoreParams) {
            if (Files.exists(Paths.get(pkcs12KeyStoreParams.getLocation()))) {
                LOG.info("An keystore does already exist. Delete it and try again.");
                return;
            }
        }
        // TODO: Check for example in PKCS#11 if the alias already exists

        {
            Path rootCaDir = filesDir.resolve("_rootCA");
            if (!Files.exists(rootCaDir) && !Files.isDirectory(rootCaDir)) {
                LOG.error("Cannot proceed. RootCA folder does not exist.");
                return;
            }

            Path caCertificatePath = rootCaDir.resolve("root_ca_certificate.pem");
            Path caPublicKeyPath = rootCaDir.resolve("public_key.pem");
            Path caPrivateKeyPath = rootCaDir.resolve("private_key.pem");

            if (!Files.exists(caCertificatePath) || !Files.exists(caPublicKeyPath) || !Files.exists(caPrivateKeyPath)) {
                LOG.error("At least one of the required CA files were not found. Unable to proceed");
                return;
            }

            LOG.info("Loading CA KeyPair and Certificate Bytes into memory");
            KeyPair caKeyPair = PemUtil.loadKeyPair(caPrivateKeyPath, caPublicKeyPath);
            byte[] caCertificateBytes = CertTools.getCertificateBytes(caCertificatePath, caKeyPair);
            X509Certificate caCertificate = X509.convertToX509Cert(caCertificateBytes);

            LOG.info("Adding " + CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA + " to KeyStore");
            keyStore.setKeyEntry(
                    CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA,
                    caKeyPair.getPrivate(),
                    "".toCharArray(),
                    new X509Certificate[]{
                            caCertificate
                    }
            );

            for (ProvisionerConfig config : appConfig.getProvisioner()) {
                final String provisionerName = config.getName();
                final Path intermediateProvisionerPath = filesDir.resolve(provisionerName);
                final String KeyStoreAliasName = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);

                final Path intermediateKeyPairPublicFile = intermediateProvisionerPath.resolve("public_key.pem");
                final Path intermediateKeyPairPrivateFile = intermediateProvisionerPath.resolve("private_key.pem");
                final Path intermediateCertificateFile = intermediateProvisionerPath.resolve("certificate.pem");

                if (!Files.exists(intermediateKeyPairPublicFile) || !Files.exists(intermediateKeyPairPrivateFile) || !Files.exists(
                        intermediateCertificateFile)) {
                    LOG.warn("Cannot use intermediate {} cause not all required files are existing.", provisionerName);

                    // Use next provisioner, we cannot import the certificates
                    continue;
                }

                LOG.info("Loading Intermediate CA for provisioner {} from disk", provisionerName);
                LOG.info("Loading Key Pair");
                KeyPair intermediateKeyPair = PemUtil.loadKeyPair(intermediateKeyPairPrivateFile, intermediateKeyPairPublicFile);
                LOG.info("Loading Intermediate CA certificate");
                byte[] intermediateCertificateBytes = CertTools.getCertificateBytes(intermediateCertificateFile, intermediateKeyPair);
                X509Certificate intermediateCertificate = X509.convertToX509Cert(intermediateCertificateBytes);

                LOG.info("Adding {} to KeyStore", KeyStoreAliasName);
                X509Certificate[] chain = new X509Certificate[]{
                        intermediateCertificate, caCertificate
                };
                keyStore.setKeyEntry(
                        KeyStoreAliasName,
                        intermediateKeyPair.getPrivate(),
                        "".toCharArray(),
                        chain
                );
            }

            cryptoStoreManager.saveKeystore();
            LOG.info("Finished! Migration was successful");
        }
    }

    /**
     * Private constructor to prevent object initiation
     */
    private KSMigrationTool() {
    }
}
