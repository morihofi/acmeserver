package de.morihofi.acmeserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.*;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.download.DownloadCaEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.FinalizeOrderEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.OrderCertEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.OrderInfoEndpoint;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRL;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRLEndpoint;
import de.morihofi.acmeserver.certificate.revokeDistribution.OcspEndpointGet;
import de.morihofi.acmeserver.certificate.revokeDistribution.OcspEndpointPost;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.config.certificateAlgorithms.AlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.EcdsaAlgorithmParams;
import de.morihofi.acmeserver.config.certificateAlgorithms.RSAAlgorithmParams;
import de.morihofi.acmeserver.config.helper.AlgorithmParamsDeserializer;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS11KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig.PKCS12KeyStoreConfig;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewInitializer;
import de.morihofi.acmeserver.tools.network.JettySslHelper;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewWatcher;
import de.morihofi.acmeserver.tools.regex.ConfigCheck;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Main {

    public static final Logger log = LogManager.getLogger(Main.class);

    public static final Path FILES_DIR = Paths.get("serverdata").toAbsolutePath();

    public static CryptoStoreManager cryptoStoreManager;

    //Build Metadata
    public static String buildMetadataVersion;
    public static String buildMetadataBuildTime;
    public static String buildMetadataGitCommit;

    public static Config appConfig;

    public static void main(String[] args) throws Exception {
        Gson configGson = new GsonBuilder().registerTypeAdapter(AlgorithmParams.class, new AlgorithmParamsDeserializer()).create();

        printBanner();

        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        //Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Providers");
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastleJsseProvider());

        Path configPath = FILES_DIR.resolve("settings.json");
        ensureFilesDirectoryExists(configPath);

        log.info("Loading server configuration");
        appConfig = configGson.fromJson(Files.readString(configPath), Config.class);

        loadBuildAndGitMetadata();
        initializeDatabaseDrivers();
        cryptoStoreManager = new CryptoStoreManager(
               //new PKCS11KeyStoreConfig(Paths.get("C:\\SoftHSM2\\lib\\softhsm2-x64.dll"), 0, "1234")
                new PKCS12KeyStoreConfig(Paths.get("test.p12"), "test")
        );
        initializeCA(cryptoStoreManager);

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

            log.info("API Call [" + ctx.method() + "] " + ctx.path());
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
            log.error("ACME Exception thrown: " + exception.getErrorResponse().getDetail() + " (" + exception.getErrorResponse().getType() + ")");
        });


        // Global routes
        app.get("/serverinfo", new ServerInfoEndpoint(appConfig.getProvisioner()));
        app.get("/ca.crt", new DownloadCaEndpoint(cryptoStoreManager));

        List<Provisioner> provisioners = getProvisioners(appConfig.getProvisioner(), app, cryptoStoreManager);


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
    private static List<Provisioner> getProvisioners(List<ProvisionerConfig> provisionerConfigList, Javalin javalinInstance, CryptoStoreManager cryptoStoreManager) throws Exception {

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
                    log.info("Generating RSA " + rsaParams.getKeySize() + "bit Key Pair for Intermediate CA");
                    intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(), cryptoStoreManager.getKeyStore().getProvider().getName());
                }
                if (config.getIntermediate().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                    log.info("Using ECDSA algorithm (Elliptic curves");

                    log.info("Generating ECDSA Key Pair using curve " + ecdsaAlgorithmParams.getCurveName() + " for Intermediate CA");
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

            } else {
                log.info("Loading Intermediate CA and KeyPair for provisioner " + provisionerName);
                intermediateKeyPair = cryptoStoreManager.getIntermediateCerificateAuthorityKeyPair(provisionerName);
                intermediateCertificate = (X509Certificate) cryptoStoreManager.getKeyStore().getCertificate(IntermediateKeyAlias);
            }
            provisioner.setIntermediateCaKeyPair(intermediateKeyPair);
            provisioner.setIntermediateCaCertificate(intermediateCertificate);


            // Initialize the CertificateRenewWatcher for this provisioner
            KeyStore keyStore = cryptoStoreManager.getKeyStore();
            KeyPair intermediateKeyPair2 = new KeyPair(
                    keyStore.getCertificate(IntermediateKeyAlias).getPublicKey(),
                    (PrivateKey) keyStore.getKey(IntermediateKeyAlias, "".toCharArray())
            );
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(IntermediateKeyAlias);
            CertificateRenewInitializer.initializeIntermediateCertificateRenewWatcher(cryptoStoreManager, IntermediateKeyAlias, provisioner, config, intermediateKeyPair2, certificate);


            if (config.isUseThisProvisionerIntermediateForAcmeApi()) {

                generateAcmeApiClientCertificate(cryptoStoreManager, provisionerName, provisioner);


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


                            // Laden des Schlüsselpaares und des Zertifikats aus dem KeyStore
                            KeyPair keyPair = new KeyPair(
                                    keyStore.getCertificate(IntermediateKeyAlias).getPublicKey(),
                                    (PrivateKey) keyStore.getKey(IntermediateKeyAlias, "".toCharArray())
                            );

                            byte[] itmCertificateBytes = keyStore.getCertificate(IntermediateKeyAlias).getEncoded();



                            //Generate new certificate in place
                            generateAcmeApiClientCertificate(cryptoStoreManager, provisionerName, provisioner);

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

            //Set the missing values
            provisioner.setIntermediateCaCertificate(intermediateCertificate);
            provisioner.setIntermediateCaKeyPair(intermediateKeyPair);

            provisioners.add(provisioner);


        }
        return provisioners;
    }

    /**
     * Generates or loads the ACME Web API client certificate and key pair.
     *
     * @param provisioner             The provisioner for certificate generation.
     * @throws CertificateException      If an issue occurs during certificate generation or loading.
     * @throws IOException               If an I/O error occurs while creating or deleting files.
     * @throws NoSuchAlgorithmException  If the specified algorithm is not available.
     * @throws NoSuchProviderException   If the specified security provider is not available.
     * @throws OperatorCreationException If there's an issue with operator creation during certificate generation.
     */
    private static void generateAcmeApiClientCertificate(CryptoStoreManager cryptoStoreManager, String provisionerName, Provisioner provisioner) throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, KeyStoreException, UnrecoverableKeyException {
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


    /**
     * Prints a banner with a stylized text art representation.
     */
    private static void printBanner() {
        System.out.println("""
                    _                       ____                          \s
                   / \\   ___ _ __ ___   ___/ ___|  ___ _ ____   _____ _ __\s
                  / _ \\ / __| '_ ` _ \\ / _ \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|
                 / ___ \\ (__| | | | | |  __/___) |  __/ |   \\ V /  __/ |  \s
                /_/   \\_\\___|_| |_| |_|\\___|____/ \\___|_|    \\_/ \\___|_|  \s
                """);
    }

    /**
     * Ensures that the necessary files directory and configuration file exist.
     *
     * @throws IOException If an I/O error occurs while creating directories or checking for the configuration file.
     */
    private static void ensureFilesDirectoryExists(Path configPath) throws IOException {
        if (!Files.exists(FILES_DIR)) {
            log.info("First run detected, creating settings directory");
            Files.createDirectories(FILES_DIR);
        }
        if (!Files.exists(configPath)) {
            log.fatal("No configuration was found. Please create a file called \"settings.json\" in \"" + FILES_DIR.toAbsolutePath() + "\". Then try again");
            System.exit(1);
        }
    }

    /**
     * Loads build and Git metadata from resource files and populates corresponding variables.
     */
    private static void loadBuildAndGitMetadata() {
        try (InputStream is = Main.class.getResourceAsStream("/build.properties")) {
            if (is != null) {
                Properties buildMetadataProperties = new Properties();
                buildMetadataProperties.load(is);
                buildMetadataVersion = buildMetadataProperties.getProperty("build.version");
                buildMetadataBuildTime = buildMetadataProperties.getProperty("build.date") + " UTC";
            } else {
                log.warn("Unable to load build metadata");
            }
        } catch (Exception e) {
            log.error("Unable to load build metadata", e);
        }
        try (InputStream is = Main.class.getResourceAsStream("/git.properties")) {
            if (is != null) {
                Properties gitMetadataProperties = new Properties();
                gitMetadataProperties.load(is);
                buildMetadataGitCommit = gitMetadataProperties.getProperty("git.commit.id.full");
            } else {
                log.warn("Unable to load git metadata");
            }

        } catch (Exception e) {
            log.error("Unable to load git metadata", e);
        }
    }

    /**
     * Initializes database drivers for MariaDB and H2.
     *
     * @throws ClassNotFoundException If a database driver class is not found.
     */
    private static void initializeDatabaseDrivers() throws ClassNotFoundException {
        log.info("Loading MariaDB JDBC driver");
        Class.forName("org.mariadb.jdbc.Driver");
        log.info("Loading H2 JDBC driver");
        Class.forName("org.h2.Driver");
    }


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
    private static void initializeCA(CryptoStoreManager cryptoStoreManager) throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException, NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {


        KeyStore caKeyStore = cryptoStoreManager.getKeyStore();
        if (!caKeyStore.containsAlias(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)) {


            // Create CA

            KeyPair caKeyPair = null;
            if (appConfig.getRootCA().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                log.info("Using RSA algorithm");
                log.info("Generating RSA " + rsaParams.getKeySize() + "bit Key Pair for Root CA");
                caKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize(), caKeyStore.getProvider().getName());
            }
            if (appConfig.getRootCA().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                log.info("Using ECDSA algorithm (Elliptic curves");

                log.info("Generating ECDSA Key Pair using curve " + ecdsaAlgorithmParams.getCurveName() + " for Root CA");
                caKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName(), caKeyStore.getProvider().getName());

            }
            if (caKeyPair == null) {
                throw new IllegalArgumentException("Unknown algorithm " + appConfig.getRootCA().getAlgorithm() + " used for root certificate");
            }

            log.info("Creating CA");
            X509Certificate caCertificate = CertificateAuthorityGenerator.generateCertificateAuthorityCertificate(appConfig.getRootCA(), caKeyPair);

            // Dumping CA Certificate to HDD, so other clients can install it
            log.info("Writing CA to keystore");
            caKeyStore.setKeyEntry(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA, caKeyPair.getPrivate(), "".toCharArray(), //No password
                    new X509Certificate[]{caCertificate});
            // Save CA in Keystore
            log.info("Saving keystore");
            cryptoStoreManager.saveKeystore();
        }

    }

}