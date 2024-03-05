package de.morihofi.acmeserver;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.DirectoryEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.NewNonceEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.NewOrderEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.RevokeCertEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.download.DownloadCaEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.ServerInfoEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.FinalizeOrderEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.OrderCertEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.OrderInfoEndpoint;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRL;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRLEndpoint;
import de.morihofi.acmeserver.certificate.revokeDistribution.OcspEndpointGet;
import de.morihofi.acmeserver.certificate.revokeDistribution.OcspEndpointPost;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewInitializer;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewWatcher;
import de.morihofi.acmeserver.tools.network.JettySslHelper;
import de.morihofi.acmeserver.tools.regex.ConfigCheck;
import de.morihofi.acmeserver.webui.WebUI;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinJte;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AcmeApiServer {
    private AcmeApiServer() {
    }

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(AcmeApiServer.class);

    /**
     * Method to start the ACME API Server
     *
     * @param appConfig          Configuration instance of the ACME Server
     * @param cryptoStoreManager Instance of {@link CryptoStoreManager} for accessing KeyStores
     * @throws Exception thrown when startup fails
     */
    public static void startServer(CryptoStoreManager cryptoStoreManager, Config appConfig) throws Exception {
        log.info("Starting in Normal Mode");
        Main.initializeCA(cryptoStoreManager);

        log.info("Initializing database");
        HibernateUtil.initDatabase();

        //log.info("Initializing JTE Template Engine");
        //JavalinJte.init();

        log.info("Starting ACME API WebServer");
        Javalin app = Javalin.create(javalinConfig -> {
            //TODO: Make it compatible again with modules
            javalinConfig.staticFiles.add("/webstatic", Location.CLASSPATH); // Adjust the Location if necessary
            javalinConfig.fileRenderer(new JavalinJte());
        });


        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
            ctx.header("Access-Control-Max-Age", "3600");

            //This is for the WebUI to auto use same language as browser, if supported
            {
                String acceptLanguage = ctx.header("Accept-Language");
                Locale locale = Locale.forLanguageTag(acceptLanguage != null ? acceptLanguage.split(",")[0] : "en-US");

                ctx.attribute(WebUI.ATTR_LOCALE, locale);
            }

            log.info("API Call [{}] {}", ctx.method(), ctx.path());
        });
        app.options("/*", ctx -> {
            ctx.status(204); // No Content
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
            ctx.header("Access-Control-Max-Age", "3600");
        });

        app.exception(ACMEException.class, (exception, ctx) -> {
            Gson gson = new Gson();

            ctx.status(exception.getHttpStatusCode());
            ctx.header("Content-Type", "application/problem+json");
            //ctx.header("Link", "<" + provisioner.getApiURL() + "/directory>;rel=\"index\"");
            ctx.result(gson.toJson(exception.getErrorResponse()));
            log.error("ACME Exception thrown: {} ({})", exception.getErrorResponse().getDetail(), exception.getErrorResponse().getType());
        });


        // Global routes
        app.get("/serverinfo", new ServerInfoEndpoint(appConfig.getProvisioner()));
        app.get("/ca.crt", new DownloadCaEndpoint(cryptoStoreManager));


        List<Provisioner> provisioners = getProvisioners(appConfig.getProvisioner(), app, cryptoStoreManager, appConfig);

        WebUI.init(app, cryptoStoreManager);


        for (Provisioner provisioner : provisioners) {


            CRL crlGenerator = new CRL(provisioner);
            String prefix = "/acme/" + provisioner.getProvisionerName();

            // CRL distribution
            app.get(provisioner.getCrlPath(), new CRLEndpoint(provisioner, crlGenerator));

            // OCSP (Online Certificate Status Protocol) endpoints
            app.post(provisioner.getOcspPath(), new OcspEndpointPost(provisioner, crlGenerator));
            app.get(provisioner.getOcspPath() + "/{ocspRequest}", new OcspEndpointGet(provisioner, crlGenerator));

            // ACME Directory
            app.get(prefix + "/directory", new DirectoryEndpoint(provisioner));

            // New account
            app.post(prefix + "/acme/new-acct", new NewAccountEndpoint(provisioner));

            // TODO: Key Change Endpoint (Account key rollover)
            app.post(prefix + "/acme/key-change", new NotImplementedEndpoint());
            app.get(prefix + "/acme/key-change", new NotImplementedEndpoint());

            // New Nonce
            app.head(prefix + "/acme/new-nonce", new NewNonceEndpoint(provisioner));
            app.get(prefix + "/acme/new-nonce", new NewNonceEndpoint(provisioner));

            // Account Update
            app.post(prefix + "/acme/acct/{id}", new AccountEndpoint(provisioner));

            // Create new Order
            app.post(prefix + "/acme/new-order", new NewOrderEndpoint(provisioner));

            // Challenge / Ownership verification
            app.post(prefix + "/acme/authz/{authorizationId}", new AuthzOwnershipEndpoint(provisioner));

            // Challenge Callback
            app.post(prefix + "/acme/chall/{challengeId}/{challengeType}", new ChallengeCallbackEndpoint(provisioner));

            // Finalize endpoint
            app.post(prefix + "/acme/order/{orderId}/finalize", new FinalizeOrderEndpoint(provisioner));

            // Order info Endpoint
            app.post(prefix + "/acme/order/{orderId}", new OrderInfoEndpoint(provisioner));

            // Get Order Certificate
            app.post(prefix + "/acme/order/{orderId}/cert", new OrderCertEndpoint(provisioner));


            // Revoke certificate
            app.post(prefix + "/acme/revoke-cert", new RevokeCertEndpoint(provisioner));

            log.info("Provisioner {} registered", provisioner.getProvisionerName());
        }


        app.start();
        log.info("\u2705 Configure Routes completed. Ready for incoming requests");
        Main.startupTime = (System.currentTimeMillis() - Main.startedAt) / 1000L; //in seconds
    }


    /**
     * Retrieves or initializes provisioners based on configuration and generates ACME Web API client certificates when required.
     *
     * @param provisionerConfigList A list of provisioner configurations.
     * @param javalinInstance       The Javalin instance.
     * @param appConfig             Configuration instance of the ACME Server
     * @param cryptoStoreManager    Instance of {@link CryptoStoreManager} for accessing KeyStores
     * @return A list of provisioners.
     * @throws Exception If an error occurs during provisioning or certificate generation.
     */
    private static List<Provisioner> getProvisioners(List<ProvisionerConfig> provisionerConfigList, Javalin javalinInstance, CryptoStoreManager cryptoStoreManager, Config appConfig) throws Exception {

        List<Provisioner> provisioners = new ArrayList<>();

        for (ProvisionerConfig config : provisionerConfigList) {
            String provisionerName = config.getName();

            if (!ConfigCheck.isValidProvisionerName(provisionerName)) {
                throw new IllegalArgumentException("Invalid provisioner name in config. Can only contain a-z, numbers, \"-\" and \"_\"");
            }
            final String IntermediateKeyAlias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);

            KeyPair intermediateKeyPair = null;
            X509Certificate intermediateCertificate;
            final Provisioner provisioner = new Provisioner(provisionerName, config.getMeta(), config.getIssuedCertificateExpiration(), config.getDomainNameRestriction(), config.isWildcardAllowed(), cryptoStoreManager);


            //Check if root ca does exist
            assert cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);

            if (!cryptoStoreManager.getKeyStore().containsAlias(IntermediateKeyAlias)) {

                // *****************************************
                // Create Intermediate Certificate


                if (config.getIntermediate().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                    log.info("Using RSA algorithm");
                    log.info("Generating RSA {} bit Key Pair for Intermediate CA", rsaParams.getKeySize());
                    intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(), cryptoStoreManager.getKeyStore().getProvider().getName());
                }
                if (config.getIntermediate().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                    log.info("Using ECDSA algorithm (Elliptic curves");

                    log.info("Generating ECDSA Key Pair using curve {} for Intermediate CA", ecdsaAlgorithmParams.getCurveName());
                    intermediateKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName(), cryptoStoreManager.getKeyStore().getProvider().getName());

                }
                if (intermediateKeyPair == null) {
                    throw new IllegalArgumentException("Unknown algorithm " + config.getIntermediate().getAlgorithm() + " used for intermediate certificate in provisioner " + provisionerName);
                }


                log.info("Generating Intermediate CA");
                intermediateCertificate = CertificateAuthorityGenerator.createIntermediateCaCertificate(cryptoStoreManager, intermediateKeyPair, config.getIntermediate().getMetadata(), config.getIntermediate().getExpiration(), provisioner.getFullCrlUrl(), provisioner.getFullOcspUrl());
                log.info("Storing generated Intermedia CA");
                X509Certificate[] chain = new X509Certificate[]{intermediateCertificate, (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)};
                cryptoStoreManager.getKeyStore().setKeyEntry(
                        IntermediateKeyAlias,
                        intermediateKeyPair.getPrivate(),
                        "".toCharArray(),
                        chain
                );
                log.info("Saving KeyStore");
                cryptoStoreManager.saveKeystore();

            }
            // Initialize the CertificateRenewWatcher for this provisioner
            KeyStore keyStore = cryptoStoreManager.getKeyStore();

            CertificateRenewInitializer.initializeIntermediateCertificateRenewWatcher(cryptoStoreManager, IntermediateKeyAlias, provisioner, config);


            if (config.isUseThisProvisionerIntermediateForAcmeApi()) {

                generateAcmeApiClientCertificate(cryptoStoreManager, provisioner, appConfig);

                log.info("Updating Javalin's TLS configuration");

                int httpPort = appConfig.getServer().getPorts().getHttp();
                int httpsPort = appConfig.getServer().getPorts().getHttps();

                /*
                 * Why we don't use Javalin's official SSL Plugin?
                 * The official SSL plugin depends on Google's Conscrypt provider, which uses native code
                 * and is platform dependent. This workaround implementation uses the built-in Java security
                 * libraries and Bouncy Castle, which is platform independent.
                 */

                JettySslHelper.updateSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, javalinInstance.jettyServer());

                log.info("Registering ACME API certificate expiration watcher");
                CertificateRenewWatcher watcher = new CertificateRenewWatcher(cryptoStoreManager, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, 6, TimeUnit.HOURS, () -> {
                    //Executed when certificate needs to be renewed

                    try {
                        log.info("Renewing certificate...");

                        //Generate new certificate in place
                        generateAcmeApiClientCertificate(cryptoStoreManager, provisioner, appConfig);

                        log.info("Certificate renewed successfully.");

                        log.info("Reloading ACME API certificate");

                        JettySslHelper.updateSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, javalinInstance.jettyServer());


                        log.info("Certificate reload complete");
                    } catch (Exception e) {
                        log.error("Error renewing certificates", e);
                    }
                });
                cryptoStoreManager.getCertificateRenewWatchers().add(watcher);


            }

            provisioners.add(provisioner);


        }
        return provisioners;
    }

    /**
     * Generates an ACME API client certificate for the ACME Web Server API, if it doesn't already exist in the key store.
     *
     * @param cryptoStoreManager The crypto store manager used for managing certificates and keys.
     * @param provisioner        The provisioner associated with the certificate generation.
     * @param appConfig          The application configuration containing settings for the ACME API and certificates.
     * @throws CertificateException      If there is an issue with certificate handling.
     * @throws IOException               If an I/O error occurs.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     * @throws NoSuchProviderException   If a required cryptographic provider is not available.
     * @throws OperatorCreationException If there is an issue creating a cryptographic operator.
     * @throws KeyStoreException         If there is an issue with the keystore.
     * @throws UnrecoverableKeyException If a keystore key cannot be recovered.
     */
    private static void generateAcmeApiClientCertificate(CryptoStoreManager cryptoStoreManager, Provisioner provisioner, Config appConfig) throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, KeyStoreException, UnrecoverableKeyException {
        String intermediateCaAlias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisioner.getProvisionerName());

        KeyPair intermediateCaKeyPair = cryptoStoreManager.getIntermediateCerificateAuthorityKeyPair(provisioner.getProvisionerName());

        KeyPair acmeAPIKeyPair;
        if (!cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI) ||
                (cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI) &&
                        !isCertificateValid(((X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA))))
        ) {

            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)
            log.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
            acmeAPIKeyPair = KeyPairGenerator.generateRSAKeyPair(4096, cryptoStoreManager.getKeyStore().getProvider().getName());

            log.info("Using provisioner intermediate CA for generation");

            log.info("Creating Server Certificate");
            X509Certificate rootCertificate = (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA);
            X509Certificate intermediateCertificate = (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(intermediateCaAlias);
            X509Certificate acmeAPICertificate = ServerCertificateGenerator.createServerCertificate(
                    intermediateCaKeyPair,
                    intermediateCertificate,
                    acmeAPIKeyPair.getPublic().getEncoded(),
                    new String[]{
                            appConfig.getServer().getDnsName()
                    },
                    provisioner);

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

    private static boolean isCertificateValid(X509Certificate certificate) {
        try {
            certificate.checkValidity(new Date());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }


}
