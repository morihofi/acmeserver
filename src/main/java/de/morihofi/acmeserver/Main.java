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
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.X509;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateAuthorityGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.KeyPairGenerator;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewInitializer;
import de.morihofi.acmeserver.tools.network.JettySslHelper;
import de.morihofi.acmeserver.tools.certificate.renew.watcher.CertificateRenewWatcher;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

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

    //CA Certificate
    public static Path caPrivateKeyPath;
    public static Path caPublicKeyPath;
    public static Path caCertificatePath;
    private static KeyPair caKeyPair;
    public static byte[] caCertificateBytes;


    //Build Metadata
    public static String buildMetadataVersion;
    public static String buildMetadataBuildTime;
    public static String buildMetadataGitCommit;

    public static Config appConfig;

    public static void main(String[] args) throws Exception {
        Gson configGson = new GsonBuilder()
                .registerTypeAdapter(AlgorithmParams.class, new AlgorithmParamsDeserializer())
                .create();

        printBanner();

        //Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Provider");
        Security.addProvider(new BouncyCastleProvider());

        Path configPath = FILES_DIR.resolve("settings.json");
        ensureFilesDirectoryExists(configPath);

        log.info("Loading server configuration");
        appConfig = configGson.fromJson(Files.readString(configPath), Config.class);

        loadBuildAndGitMetadata();
        initializeDatabaseDrivers();
        initializeCA();

        log.info("Initializing database");
        HibernateUtil.initDatabase();

        log.info("Starting ACME API WebServer");
        Javalin app = Javalin.create(javalinConfig -> {
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
        app.get("/ca.crt", new DownloadCaEndpoint());

        List<Provisioner> provisioners = getProvisioners(appConfig.getProvisioner(), app);


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
    private static List<Provisioner> getProvisioners(List<ProvisionerConfig> provisionerConfigList, Javalin javalinInstance) throws Exception {

        List<Provisioner> provisioners = new ArrayList<>();

        for (ProvisionerConfig config : provisionerConfigList) {
            String provisionerName = config.getName();
            Path intermediateProvisionerPath = FILES_DIR.resolve(provisionerName);

            Path intermediateKeyPairPublicFile = intermediateProvisionerPath.resolve("public_key.pem");
            Path intermediateKeyPairPrivateFile = intermediateProvisionerPath.resolve("private_key.pem");
            Path intermediateCertificateFile = intermediateProvisionerPath.resolve("certificate.pem");

            Provisioner provisioner = new Provisioner(provisionerName, null, null, config.getMeta(), config.getIssuedCertificateExpiration(), config.getDomainNameRestriction(), config.isWildcardAllowed());

            X509Certificate intermediateCertificate;
            KeyPair intermediateKeyPair = null;
            if (!Files.exists(intermediateKeyPairPublicFile) || !Files.exists(intermediateKeyPairPrivateFile) || !Files.exists(intermediateCertificateFile)) {
                Files.createDirectories(intermediateProvisionerPath);

                //Delete all key files, if any of them exists
                Files.deleteIfExists(intermediateKeyPairPublicFile);
                Files.deleteIfExists(intermediateKeyPairPrivateFile);
                Files.deleteIfExists(intermediateCertificateFile);

                log.info("Loading Root CA Keypair for Intermediate CA generation");
                caKeyPair = PemUtil.loadKeyPair(caPrivateKeyPath, caPublicKeyPath);
                caCertificateBytes = CertTools.getCertificateBytes(caCertificatePath, caKeyPair);


                // *****************************************
                // Create Intermediate Certificate


                if (config.getIntermediate().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                    log.info("Using RSA algorithm");
                    log.info("Generating RSA " + rsaParams.getKeySize() + "bit Key Pair for Intermediate CA");
                    intermediateKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize());
                }
                if (config.getIntermediate().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                    log.info("Using ECDSA algorithm (Elliptic curves");

                    log.info("Generating ECDSA Key Pair using curve " + ecdsaAlgorithmParams.getCurveName() + " for Intermediate CA");
                    intermediateKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName());

                }
                if (intermediateKeyPair == null) {
                    throw new IllegalArgumentException("Unknown algorithm " + config.getIntermediate().getAlgorithm() + " used for intermediate certificate in provisioner " + provisionerName);
                }


                log.info("Creating Intermediate CA");
                intermediateCertificate = CertificateAuthorityGenerator.createIntermediateCaCertificate(caKeyPair, intermediateKeyPair, config.getIntermediate().getMetadata(), config.getIntermediate().getExpiration(), provisioner.getFullCrlUrl(), provisioner.getFullOcspUrl(), X509.convertToX509Cert(caCertificateBytes));

                log.info("Writing Intermediate CA KeyPair to disk");
                //KeyStoreUtils.saveAsPKCS12(intermediateKeyPair, intermediateKeyStorePassword, provisionerName, intermediateCertificate.getEncoded(), intermediateKeyStoreFilePath);
                PemUtil.saveKeyPairToPEM(intermediateKeyPair, intermediateKeyPairPublicFile, intermediateKeyPairPrivateFile);

                log.info("Writing Intermedia CA Certificate to disk");
                Files.writeString(FILES_DIR.resolve(intermediateCertificateFile), PemUtil.certificateToPEM(intermediateCertificate.getEncoded()));
            } else {
                log.info("Loading Intermediate CA for provisioner " + provisionerName + " from disk");
                log.info("Loading Key Pair");
                intermediateKeyPair = PemUtil.loadKeyPair(intermediateKeyPairPrivateFile, intermediateKeyPairPublicFile);
                log.info("Loading Intermediate CA certificate");
                byte[] intermediateCertificateBytes = CertTools.getCertificateBytes(intermediateCertificateFile, intermediateKeyPair);
                intermediateCertificate = X509.convertToX509Cert(intermediateCertificateBytes);
            }
            // Initialize the CertificateRenewWatcher for this provisioner
            CertificateRenewInitializer.initializeIntermediateCertificateRenewWatcher(intermediateKeyPairPrivateFile, intermediateKeyPairPublicFile, intermediateCertificateFile, provisioner, config, caKeyPair, X509.convertToX509Cert(caCertificateBytes));


            if (config.isUseThisProvisionerIntermediateForAcmeApi()) {
                Path acmeServerApiCertDir = FILES_DIR.resolve("_acmeAPICert");
                Files.createDirectories(acmeServerApiCertDir);

                Path acmeApiCertificatePath = acmeServerApiCertDir.resolve("certificate.pem");
                Path acmeApiPublicKeyPath = acmeServerApiCertDir.resolve("public_key.pem");
                Path acmeApiPrivateKeyPath = acmeServerApiCertDir.resolve("private_key.pem");

                generateAcmeApiClientCertificate(
                        intermediateKeyPairPrivateFile,
                        intermediateKeyPairPublicFile,
                        intermediateCertificate,
                        acmeApiCertificatePath,
                        acmeApiPublicKeyPath,
                        acmeApiPrivateKeyPath,
                        provisioner
                );


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
                            return JettySslHelper.getSslJetty(httpsPort, httpPort, acmeApiCertificatePath, acmeApiPrivateKeyPath, acmeApiPublicKeyPath);
                        } catch (Exception e) {
                            log.error("Error applying certificate to API");
                            throw new RuntimeException(e);
                        }
                    });

                    log.info("Registering ACME API certificate expiration watcher");
                    CertificateRenewWatcher watcher = new CertificateRenewWatcher(
                            acmeApiPrivateKeyPath,
                            acmeApiPublicKeyPath,
                            acmeApiCertificatePath,
                            6, TimeUnit.HOURS,
                            () -> {
                                //Executed when certificate needs to be renewed

                                try {
                                    log.info("Renewing certificate...");

                                    // Loading intermediate certificates from file, because
                                    // they can have renewed in the meantime and variables not updated

                                    KeyPair keyPair = PemUtil.loadKeyPair(intermediateKeyPairPrivateFile, intermediateKeyPairPublicFile);

                                    byte[] itmCertificateBytes = CertTools.getCertificateBytes(intermediateCertificateFile, keyPair);
                                    X509Certificate itmCertificate = X509.convertToX509Cert(itmCertificateBytes);

                                    //Delete old expired certificate
                                    Files.deleteIfExists(acmeApiCertificatePath);

                                    //Generate new certificate in place
                                    generateAcmeApiClientCertificate(
                                            intermediateKeyPairPrivateFile,
                                            intermediateKeyPairPublicFile,
                                            itmCertificate,
                                            acmeApiCertificatePath,
                                            acmeApiPublicKeyPath,
                                            acmeApiPrivateKeyPath,
                                            provisioner
                                    );

                                    log.info("Certificate renewed successfully.");

                                    log.info("Reloading ACME API certificate");

                                    javalinConfig.jetty.server(() -> {
                                        try {
                                            return JettySslHelper.getSslJetty(httpsPort, httpPort, acmeApiCertificatePath, acmeApiPrivateKeyPath, acmeApiPublicKeyPath);
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
     * @param intermediateKeyPairPrivateFile The path to the intermediate CA's private key file.
     * @param intermediateKeyPairPublicFile  The path to the intermediate CA's public key file.
     * @param intermediateCertificate        The intermediate CA certificate.
     * @param acmeApiCertificatePath         The path to the ACME Web API client certificate file.
     * @param acmeApiPublicKeyPath           The path to the ACME Web API client's public key file.
     * @param acmeApiPrivateKeyPath          The path to the ACME Web API client's private key file.
     * @param provisioner                    The provisioner for certificate generation.
     * @throws CertificateException      If an issue occurs during certificate generation or loading.
     * @throws IOException               If an I/O error occurs while creating or deleting files.
     * @throws NoSuchAlgorithmException  If the specified algorithm is not available.
     * @throws NoSuchProviderException   If the specified security provider is not available.
     * @throws OperatorCreationException If there's an issue with operator creation during certificate generation.
     */
    private static void generateAcmeApiClientCertificate(Path intermediateKeyPairPrivateFile, Path intermediateKeyPairPublicFile, X509Certificate intermediateCertificate, Path acmeApiCertificatePath, Path acmeApiPublicKeyPath, Path acmeApiPrivateKeyPath, Provisioner provisioner) throws CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException {
        KeyPair intermediateKeyPair = PemUtil.loadKeyPair(intermediateKeyPairPrivateFile, intermediateKeyPairPublicFile);

        KeyPair acmeAPIKeyPair;
        if (!Files.exists(acmeApiPublicKeyPath) || !Files.exists(acmeApiPrivateKeyPath)) {
            Files.deleteIfExists(acmeApiCertificatePath);
            Files.deleteIfExists(acmeApiPublicKeyPath);
            Files.deleteIfExists(acmeApiPrivateKeyPath);

            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)
            log.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
            acmeAPIKeyPair = KeyPairGenerator.generateRSAKeyPair(4096);
            // Save keyPair to disk
            log.info("Writing keyPair to disk");
            PemUtil.saveKeyPairToPEM(acmeAPIKeyPair, acmeApiPublicKeyPath, acmeApiPrivateKeyPath);

        } else {
            log.info("Loading KeyPair for ACME Web API from disk");
            acmeAPIKeyPair = PemUtil.loadKeyPair(acmeApiPrivateKeyPath, acmeApiPublicKeyPath);
        }


        if (!Files.exists(acmeApiCertificatePath)) {
            log.info("Using provisioner intermediate CA for generation");

            log.info("Creating Server Certificate");
            X509Certificate acmeAPICertificate = ServerCertificateGenerator.createServerCertificate(intermediateKeyPair, intermediateCertificate.getEncoded(), acmeAPIKeyPair.getPublic().getEncoded(), new String[]{appConfig.getServer().getDnsName()}, provisioner);

            // Dumping certificate to HDD
            log.info("Writing certificate to disk");
            byte[][] certificates = new byte[][]{acmeAPICertificate.getEncoded(), intermediateCertificate.getEncoded()};
            String pemCertificates = PemUtil.certificatesChainToPEM(certificates);
            Files.createFile(acmeApiCertificatePath);
            Files.writeString(acmeApiCertificatePath, pemCertificates);
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
    private static void initializeCA() throws NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException, NoSuchProviderException, InvalidAlgorithmParameterException {
        Path rootCaDir = FILES_DIR.resolve("_rootCA");
        Files.createDirectories(rootCaDir);

        caCertificatePath = rootCaDir.resolve("root_ca_certificate.pem");
        caPublicKeyPath = rootCaDir.resolve("public_key.pem");
        caPrivateKeyPath = rootCaDir.resolve("private_key.pem");

        if (!Files.exists(caCertificatePath) || !Files.exists(caPublicKeyPath) || !Files.exists(caPrivateKeyPath)) {

            Files.deleteIfExists(caCertificatePath);
            Files.deleteIfExists(caPublicKeyPath);
            Files.deleteIfExists(caPrivateKeyPath);

            // Create CA

            KeyPair caKeyPair = null;
            if (appConfig.getRootCA().getAlgorithm() instanceof RSAAlgorithmParams rsaParams) {
                log.info("Using RSA algorithm");
                log.info("Generating RSA " + rsaParams.getKeySize() + "bit Key Pair for Root CA");
                caKeyPair = KeyPairGenerator.generateRSAKeyPair(rsaParams.getKeySize());
            }
            if (appConfig.getRootCA().getAlgorithm() instanceof EcdsaAlgorithmParams ecdsaAlgorithmParams) {
                log.info("Using ECDSA algorithm (Elliptic curves");

                log.info("Generating ECDSA Key Pair using curve " + ecdsaAlgorithmParams.getCurveName() + " for Root CA");
                caKeyPair = KeyPairGenerator.generateEcdsaKeyPair(ecdsaAlgorithmParams.getCurveName());

            }
            if (caKeyPair == null) {
                throw new IllegalArgumentException("Unknown algorithm " + appConfig.getRootCA().getAlgorithm() + " used for root certificate");
            }

            log.info("Creating CA");
            caCertificateBytes = CertificateAuthorityGenerator.generateCertificateAuthorityCertificate(appConfig.getRootCA(), caKeyPair).getEncoded();

            // Dumping CA Certificate to HDD, so other clients can install it
            log.info("Writing CA to disk");
            Files.createFile(caCertificatePath);
            Files.writeString(FILES_DIR.resolve(caCertificatePath), PemUtil.certificateToPEM(caCertificateBytes));
            // Save CA in Keystore
            log.info("Writing CA to disk");
            PemUtil.saveKeyPairToPEM(caKeyPair, caPublicKeyPath, caPrivateKeyPath);
        } else {
            log.info("Loading CA KeyPair and Certificate Bytes into memory");
            caKeyPair = PemUtil.loadKeyPair(caPrivateKeyPath, caPublicKeyPath);
            caCertificateBytes = CertTools.getCertificateBytes(caCertificatePath, caKeyPair);
        }

    }

}