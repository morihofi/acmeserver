package de.morihofi.acmeserver;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.*;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.download.DownloadCaEndpoint;
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
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AcmeApiServer {
    public static final Logger log = LogManager.getLogger(AcmeApiServer.class);

    public static void startServer(CryptoStoreManager cryptoStoreManager, Config appConfig) throws Exception {
        log.info("Starting in Normal Mode");
        Main.initializeCA(cryptoStoreManager);

        log.info("Initializing database");
        HibernateUtil.initDatabase();

        log.info("Starting ACME API WebServer");
        Javalin app = Javalin.create(javalinConfig -> {
            //TODO: Make it compatible again with modules
            javalinConfig.staticFiles.add("/webstatic", Location.CLASSPATH); // Adjust the Location if necessary
        });


        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
            ctx.header("Access-Control-Max-Age", "3600");

            log.info("API Call [{}}] {}", ctx.method(), ctx.path());
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


        for (Provisioner provisioner : provisioners) {


            CRL crlGenerator = new CRL(provisioner);
            String prefix = "/" + provisioner.getProvisionerName();

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

            log.info("Provisioner " + provisioner.getProvisionerName() + " registered");
        }
        app.start();
        log.info("\u2705 Configure Routes completed. Ready for incoming requests");
    }


    /**
     * Retrieves or initializes provisioners based on configuration and generates ACME Web API client certificates when required.
     *
     * @param provisionerConfigList A list of provisioner configurations.
     * @param javalinInstance       The Javalin instance.
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
            final Provisioner provisioner = new Provisioner(provisionerName, null, null, config.getMeta(), config.getIssuedCertificateExpiration(), config.getDomainNameRestriction(), config.isWildcardAllowed(), cryptoStoreManager);


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
            KeyPair intermediateKeyPair2 = new KeyPair(
                    keyStore.getCertificate(IntermediateKeyAlias).getPublicKey(),
                    (PrivateKey) keyStore.getKey(IntermediateKeyAlias, "".toCharArray())
            );
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(IntermediateKeyAlias);
            CertificateRenewInitializer.initializeIntermediateCertificateRenewWatcher(cryptoStoreManager, IntermediateKeyAlias, provisioner, config, intermediateKeyPair2, certificate);


            if (config.isUseThisProvisionerIntermediateForAcmeApi()) {

                generateAcmeApiClientCertificate(cryptoStoreManager, provisionerName, provisioner, appConfig);


                javalinInstance.updateConfig(javalinConfig -> {
                    log.info("Updating Javalin's TLS configuration");

                    int httpPort = appConfig.getServer().getPorts().getHttp();
                    int httpsPort = appConfig.getServer().getPorts().getHttps();

                    /*
                     * Why we don't use Javalin's official SSL Plugin?
                     * The official SSL plugin depends on Google's Conscrypt provider, which uses native code
                     * and is platform dependent. This workaround implementation uses the built-in Java security
                     * libraries and Bouncy Castle, which is platform independent.
                     */

                    javalinConfig.jetty.server(() -> {
                        try {
                            return JettySslHelper.getSslJetty(httpsPort, httpPort, cryptoStoreManager.getKeyStore(), CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI);
                        } catch (Exception e) {
                            log.error("Error applying certificate to API");
                            throw new RuntimeException(e);
                        }
                    });

                    log.info("Registering ACME API certificate expiration watcher");
                    CertificateRenewWatcher watcher = new CertificateRenewWatcher(cryptoStoreManager, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI, 6, TimeUnit.HOURS, () -> {
                        //Executed when certificate needs to be renewed

                        try {
                            log.info("Renewing certificate...");

                            //Generate new certificate in place
                            generateAcmeApiClientCertificate(cryptoStoreManager, provisionerName, provisioner, appConfig);

                            log.info("Certificate renewed successfully.");

                            log.info("Reloading ACME API certificate");

                            javalinConfig.jetty.server(() -> {
                                try {
                                    return JettySslHelper.getSslJetty(httpsPort, httpPort, keyStore, CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI);
                                } catch (Exception e) {
                                    log.error("Error applying new certificate to API");
                                    throw new RuntimeException(e);
                                }
                            });
                            log.info("Certificate reload complete");
                        } catch (Exception e) {
                            log.error("Error renewing certificates", e);
                        }
                    });
                });
            }

            provisioners.add(provisioner);


        }
        return provisioners;
    }

    /**
     * Generates or loads the ACME Web API client certificate and key pair.
     *
     * @param provisioner The provisioner for certificate generation.
     * @throws CertificateException      If an issue occurs during certificate generation or loading.
     * @throws IOException               If an I/O error occurs while creating or deleting files.
     * @throws NoSuchAlgorithmException  If the specified algorithm is not available.
     * @throws NoSuchProviderException   If the specified security provider is not available.
     * @throws OperatorCreationException If there's an issue with operator creation during certificate generation.
     */
    private static void generateAcmeApiClientCertificate(CryptoStoreManager cryptoStoreManager, String provisionerName, Provisioner provisioner, Config appConfig) throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, KeyStoreException, UnrecoverableKeyException {
        String intermediateCaAlias = CryptoStoreManager.getKeyStoreAliasForProvisionerIntermediate(provisionerName);

        KeyPair intermediateCaKeyPair = cryptoStoreManager.getIntermediateCerificateAuthorityKeyPair(provisionerName);

        KeyPair acmeAPIKeyPair;
        if (!cryptoStoreManager.getKeyStore().containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ACMEAPI)) {

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
