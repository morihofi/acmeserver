package de.morihofi.acmeserver.tools;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewWatcher;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import de.morihofi.acmeserver.tools.network.JettySslHelper;
import io.javalin.Javalin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class JavalinSecurityHelper {

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().getClass());


    /**
     * Initializes secure API settings for a Javalin application, configuring SSL/TLS using a custom
     * mechanism rather than Javalin's official SSL plugin. This method involves setting up the Jetty server
     * underlying Javalin to use SSL certificates managed by a provided {@link CryptoStoreManager}, and
     * establishing a mechanism to automatically renew the ACME API certificate.
     *
     * <p>The method first generates an ACME API client certificate using the {@link CryptoStoreManager} and
     * the application configuration. It then logs the update to Javalin's TLS configuration and retrieves
     * the HTTP and HTTPS ports from the application configuration. Instead of using the official SSL plugin
     * (which depends on Google's Conscrypt provider and is platform-dependent), this method utilizes Java's
     * built-in security libraries and Bouncy Castle for platform independence.</p>
     *
     * <p>It updates the SSL configuration of the Jetty server to use the newly generated certificate and
     * sets up a {@link CertificateRenewWatcher} to monitor certificate expiration. This watcher is configured
     * to renew the certificate automatically before it expires and reload the certificate in the Jetty server
     * without requiring a restart of the application.</p>
     *
     * @param app                the Javalin application instance to be configured.
     * @param cryptoStoreManager the manager responsible for cryptographic operations and storage.
     * @param appConfig          the configuration object containing application settings, including server port information.
     * @throws Exception if there is an error in generating the ACME API client certificate, updating the Jetty
     *                   server SSL configuration, or during automatic certificate renewal.
     */
    public static void initSecureApi(Javalin app, CryptoStoreManager cryptoStoreManager, Config appConfig) throws Exception {
        KeyStore keyStore = cryptoStoreManager.getKeyStore();

        generateAcmeApiClientCertificate(cryptoStoreManager, appConfig);

        log.info("Updating Javalin's TLS configuration");

        int httpPort = appConfig.getServer().getPorts().getHttp();
        int httpsPort = appConfig.getServer().getPorts().getHttps();
        boolean enableSniCheck = appConfig.getServer().isEnableSniCheck();

        /*
         * Why we don't use Javalin's official SSL Plugin?
         * The official SSL plugin depends on Google's Conscrypt provider, which uses native code
         * and is platform dependent. This workaround implementation uses the built-in Java security
         * libraries and Bouncy Castle, which is platform independent.
         */

        JettySslHelper.updateSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, app.jettyServer(), enableSniCheck);

        log.info("Registering ACME API certificate expiration watcher");
        CertificateRenewWatcher watcher = new CertificateRenewWatcher(cryptoStoreManager, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, 6, TimeUnit.HOURS, () -> {
            //Executed when certificate needs to be renewed

            try {
                log.info("Renewing certificate...");

                //Generate new certificate in place
                generateAcmeApiClientCertificate(cryptoStoreManager, appConfig);

                log.info("Certificate renewed successfully, now reloading ACME API certificate");

                JettySslHelper.updateSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, app.jettyServer(), enableSniCheck);


                log.info("Certificate reload complete");
            } catch (Exception e) {
                log.error("Error renewing certificates", e);
            }
        });
        cryptoStoreManager.getCertificateRenewWatchers().add(watcher);
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
    private static void generateAcmeApiClientCertificate(CryptoStoreManager cryptoStoreManager, Config appConfig) throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, KeyStoreException, UnrecoverableKeyException {
        String rootCaAlias = CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA;

        KeyPair rootCaKeyPair = cryptoStoreManager.getCerificateAuthorityKeyPair();

        KeyPair acmeAPIKeyPair;
        if (!cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI) ||
                (cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI) &&
                        !CertTools.isCertificateValid(((X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI))))
        ) {

            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)


            log.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
            acmeAPIKeyPair = KeyPairGenerator.generateRSAKeyPair(4096, cryptoStoreManager.getKeyStore().getProvider().getName());

            log.info("Using root CA for generation");
            X509Certificate rootCertificate = (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);
            X509Certificate intermediateCertificate = (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(rootCaAlias);

            log.info("Creating Server Certificate");
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
                            new Identifier(Identifier.IDENTIFIER_TYPE.DNS.name(), appConfig.getServer().getDnsName())
                    },
                    startDate,
                    endDate,
                    null
            );

            // Dumping certificate to HDD
            log.info("Storing certificate in KeyStore");
            X509Certificate[] chain = new X509Certificate[]{
                    acmeAPICertificate,
                    intermediateCertificate,
                    rootCertificate
            };

            cryptoStoreManager.getKeyStore().deleteEntry(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI);
            cryptoStoreManager.getKeyStore().setKeyEntry(
                    CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI,
                    acmeAPIKeyPair.getPrivate(),
                    "".toCharArray(),
                    chain
            );
            cryptoStoreManager.saveKeystore();
        }
    }

}
