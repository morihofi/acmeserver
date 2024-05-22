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

package de.morihofi.acmeserver.tools;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewManager;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import de.morihofi.acmeserver.tools.network.JettySslHelper;
import de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.MozillaSslConfigHelper;
import io.javalin.Javalin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class JavalinSecurityHelper {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Initializes secure API settings for a Javalin application, configuring SSL/TLS using a custom mechanism rather than Javalin's
     * official SSL plugin. This method involves setting up the Jetty server underlying Javalin to use SSL certificates managed by a
     * provided {@link CryptoStoreManager}, and establishing a mechanism to automatically renew the ACME API certificate.
     *
     * <p>The method first generates an ACME API client certificate using the {@link CryptoStoreManager} and
     * the application configuration. It then logs the update to Javalin's TLS configuration and retrieves the HTTP and HTTPS ports from the
     * application configuration. Instead of using the official SSL plugin (which depends on Google's Conscrypt provider and is
     * platform-dependent), this method utilizes Java's built-in security libraries and Bouncy Castle for platform independence.</p>
     *
     * <p>It updates the SSL configuration of the Jetty server to use the newly generated certificate and
     * sets up a watcher to monitor certificate expiration. This watcher is configured to renew the certificate automatically before it
     * expires and reload the certificate in the Jetty server without requiring a restart of the application.</p>
     *
     * @param app                the Javalin application instance to be configured.
     * @param cryptoStoreManager the manager responsible for cryptographic operations and storage.
     * @param appConfig          the configuration object containing application settings, including server port information.
     * @throws Exception if there is an error in generating the ACME API client certificate, updating the Jetty server SSL configuration, or
     *                   during automatic certificate renewal.
     */
    public static void initSecureApi(Javalin app, CryptoStoreManager cryptoStoreManager, Config appConfig,
            CertificateRenewManager certificateRenewManager) throws Exception {
        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        MozillaSslConfigHelper.BasicConfiguration mozillaSSlConfig;
        if (appConfig.getServer().getMozillaSslConfig().isEnabled()) {
            // This is needed to be able to turn on TLS 1.0, TLS 1.1 and TLS 1.2
            Security.setProperty("jdk.tls.disabledAlgorithms", "");
            Security.setProperty("jdk.certpath.disabledAlgorithms", "");

            System.setProperty("jdk.tls.allowLegacyResumption",
                    String.valueOf(appConfig.getServer().getSslServerConfig().isAllowLegacyResumption()));

            MozillaSslConfigHelper.CONFIGURATION configuration = switch (appConfig.getServer().getMozillaSslConfig().getConfiguration()) {
                case "modern" -> MozillaSslConfigHelper.CONFIGURATION.MODERN;
                case "intermediate" -> MozillaSslConfigHelper.CONFIGURATION.INTERMEDIATE;
                case "old" -> MozillaSslConfigHelper.CONFIGURATION.OLD;
                default -> throw new IllegalStateException(
                        "Unexpected value: " + appConfig.getServer().getMozillaSslConfig().getConfiguration()
                                + " must be one of modern, intermediate or old");
            };

            mozillaSSlConfig =
                    MozillaSslConfigHelper.getConfigurationGuidelinesForVersion(appConfig.getServer().getMozillaSslConfig().getVersion(),
                            MozillaSslConfigHelper.CONFIGURATION.OLD);

            LOG.info(
                    "Using Mozilla's SSL Configuration guidelines configuration {} version {} at {} with the following oldest clients "
                            + "supporting it {}",
                    configuration, mozillaSSlConfig.version(), mozillaSSlConfig.href(), mozillaSSlConfig.oldestClients());
        } else {
            mozillaSSlConfig = null;
        }

        {
            String alias = CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI;
            CertificateRenewManager.CertificateData certData = generateAcmeApiClientCertificate(cryptoStoreManager, appConfig);

            if (certData.keyPair() != null && certData.certificateChain() != null) {
                LOG.info("Saving certificate and key for alias {} in keystore", alias);
                // Save the new certificate in keystore
                keyStore.deleteEntry(alias);
                keyStore.setKeyEntry(
                        alias,
                        certData.keyPair().getPrivate(),
                        "".toCharArray(),
                        certData.certificateChain()
                );
                cryptoStoreManager.saveKeystore();
            }
        }

        LOG.info("Updating Javalin's TLS configuration");

        int httpPort = appConfig.getServer().getPorts().getHttp();
        int httpsPort = appConfig.getServer().getPorts().getHttps();
        boolean enableSniCheck = appConfig.getServer().isEnableSniCheck();

        /*
         * Why we don't use Javalin's official SSL Plugin?
         * The official SSL plugin depends on Google's Conscrypt provider, which uses native code
         * and is platform dependent. This workaround implementation uses the built-in Java security
         * libraries and Bouncy Castle, which is platform independent.
         */

        JettySslHelper.updateSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, app.jettyServer(),
                enableSniCheck, mozillaSSlConfig);

        LOG.info("Registering ACME API certificate expiration watcher");

        certificateRenewManager.registerNewCertificateRenewWatcher(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, null,
                (provisioner, x509Certificate, keyPair) -> {
                    // Generate new certificate in place
                    return generateAcmeApiClientCertificate(cryptoStoreManager, appConfig);
                }, () -> {
                    try {
                        LOG.info("Certificate renewed successfully, now reloading ACME API certificate");
                        JettySslHelper.updateSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI,
                                app.jettyServer(), enableSniCheck, mozillaSSlConfig);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Generates an ACME API client certificate for the ACME Web Server API, if it doesn't already exist in the key store.
     *
     * @param cryptoStoreManager The crypto store manager used for managing certificates and keys.
     * @param appConfig          The application configuration containing settings for the ACME API and certificates.
     * @throws CertificateException      If there is an issue with certificate handling.
     * @throws IOException               If an I/O error occurs.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     * @throws NoSuchProviderException   If a required cryptographic provider is not available.
     * @throws OperatorCreationException If there is an issue creating a cryptographic operator.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws UnrecoverableKeyException If a keystore key cannot be recovered.
     */
    private static CertificateRenewManager.CertificateData generateAcmeApiClientCertificate(CryptoStoreManager cryptoStoreManager,
            Config appConfig) throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException,
            OperatorCreationException, KeyStoreException, UnrecoverableKeyException {
        String rootCaAlias = CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA;

        KeyPair rootCaKeyPair = cryptoStoreManager.getCerificateAuthorityKeyPair();

        KeyPair acmeAPIKeyPair;
        if (!cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI) ||
                (cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI) &&
                        !CertTools.isCertificateValid(((X509Certificate) cryptoStoreManager.getKeyStore()
                                .getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI))))
        ) {

            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)

            LOG.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
            acmeAPIKeyPair = KeyPairGenerator.generateRSAKeyPair(4096, cryptoStoreManager.getKeyStore().getProvider().getName());

            LOG.info("Using root CA for generation");
            X509Certificate rootCertificate =
                    (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);
            X509Certificate intermediateCertificate = (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(rootCaAlias);

            LOG.info("Creating Server Certificate");
            Date startDate = new Date(); // Starts now
            Date endDate = DateTools.makeDateForOutliveIntermediateCertificate(
                    intermediateCertificate.getNotAfter(),
                    DateTools.addToDate(startDate,
                            0,
                            1,
                            0
                    )
            );

            X509Certificate acmeAPICertificate = ServerCertificateGenerator.createServerCertificate(
                    rootCaKeyPair,
                    intermediateCertificate,
                    acmeAPIKeyPair.getPublic().getEncoded(),
                    new Identifier[]{
                            new Identifier(Identifier.IDENTIFIER_TYPE.DNS, appConfig.getServer().getDnsName())
                    },
                    startDate,
                    endDate,
                    null
            );

            X509Certificate[] chain = new X509Certificate[]{
                    acmeAPICertificate,
                    intermediateCertificate,
                    rootCertificate
            };

            return new CertificateRenewManager.CertificateData(chain, acmeAPIKeyPair);
        } else {
            return new CertificateRenewManager.CertificateData(null, null);
        }
    }
}
