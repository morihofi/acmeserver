package de.morihofi.acmeserver;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.RevokeCertEndpoint;
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
import de.morihofi.acmeserver.config.KeyStoreConfigLoader;
import de.morihofi.acmeserver.exception.ACMEException;
import de.morihofi.acmeserver.tools.CertTools;
import de.morihofi.acmeserver.tools.KeyStoreUtils;
import de.morihofi.acmeserver.certificate.objects.KeyStoreFileContent;
import io.javalin.Javalin;
import io.javalin.community.ssl.SSLPlugin;
import io.javalin.http.staticfiles.Location;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class Main {

    public static final Logger log = LogManager.getLogger(Main.class);

    public static KeyPair intermediateKeyPair;
    public static X509Certificate intermediateCertificate;

    // Database
    public static String db_password;
    public static String db_user;
    public static String db_name;
    public static String db_host;
    public static String db_engine;

    public static final Path FILES_DIR = Paths.get("serverdata").toAbsolutePath();

    //ACME SERVER Certificate
    public static Path acmeServerKeyStorePath;
    public static String acmeServerKeyStorePassword = "";
    public static int acmeServerRSAKeyPairSize = 4096;

    //CA Certificate
    public static String caKeyStorePassword = "";
    public static String caKeyStoreAlias = "ca";
    public static Path caKeyStorePath;
    public static String caCommonName = "Root CA";
    public static int caRSAKeyPairSize = 4096;
    public static int caDefaultExpireYears = 20;
    public static Path caPath;
    private static KeyPair caKeyPair;
    public static byte[] caCertificateBytes;

    //Intermediate CA Certificate
    public static Path intermediateKeyStorePath;
    public static String intermediateKeyStorePassword = "";
    public static String intermediateKeyStoreAlias = "intermediate";
    public static String intermediateCommonName = "Intermediate CA";
    public static int intermediateRSAKeyPairSize = 4096;
    public static int intermediateDefaultExpireDays = 0;
    public static int intermediateDefaultExpireMonths = 0;
    public static int intermediateDefaultExpireYears = 180;

    //ACME Directory Information
    public static String acmeMetaWebsite = "";
    public static String acmeMetaTermsOfService = "";
    public static String acmeThisServerDNSName = "";
    public static int acmeThisServerAPIPort = 7443;

    //E-Mail SMTP
    public static int emailSMTPPort = 587;
    public static String emailSMTPEncryption = "starttls";
    public static String emailSMTPServer = "";
    public static String emailSMTPUsername = "";
    public static String emailSMTPPassword = "";
    public static String acmeAdminEmail = "";

    //ACME client created certificates
    public static int acmeCertificatesExpireDays = 1;
    public static int acmeCertificatesExpireMonths = 1;
    public static int acmeCertificatesExpireYears = 1;

    public static Properties properties = new Properties();

    //Build Metadata
    public static String buildMetadataVersion = null;
    public static String buildMetadataBuildTime = null;
    public static String buildMetadataGitCommit = null;

    public static void main(String[] args) throws Exception {

        printBanner();

        //Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Provider");
        Security.addProvider(new BouncyCastleProvider());


        ensureFilesDirectoryExists();
        Path configPath = FILES_DIR.resolve("settings.properties");
        loadConfiguration(configPath);
        loadBuildAndGitMetadata();
        initializeDatabaseDrivers();
        initializeCA();


        log.info("Loading certificates");
        KeyStoreFileContent intermediateKeyStoreContents = KeyStoreUtils.loadFromPKCS12(intermediateKeyStorePath, intermediateKeyStorePassword, intermediateKeyStoreAlias);
        log.info("Loading intermediate Key Pair");
        intermediateKeyPair = intermediateKeyStoreContents.getKeyPair();
        log.info("Loading intermediate Certificate");
        intermediateCertificate = intermediateKeyStoreContents.getCert();

        CRL crlGenerator = new CRL();

        log.info("Starting ACME API WebServer using HTTPS");
        String keyStoreLocation = acmeServerKeyStorePath.toAbsolutePath().toString();
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/webstatic", Location.CLASSPATH); // Adjust the Location if necessary
            SSLPlugin plugin = new SSLPlugin(conf -> {
                // conf.pemFromPath("/etc/ssl/certificate.pem", "/etc/ssl/privateKey.pem");
                conf.keystoreFromPath(keyStoreLocation, caKeyStorePassword);
                conf.securePort = acmeThisServerAPIPort;
                conf.insecure = false;
            });
            config.plugins.getPluginManager().register(plugin);

        }).start();


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

        //TODO: Implement Exception
        app.exception(ACMEException.class, (exception, ctx) -> {
            Gson gson = new Gson();

            ctx.status(exception.getHttpStatusCode());
            ctx.header("Content-Type", "application/problem+json");
            //ctx.header("Link", "<" + provisioner.getApiURL() + "/directory>;rel=\"index\"");
            ctx.result(gson.toJson(exception.getErrorResponse()));
        });



        // Global routes
        app.get("/serverinfo", new ServerInfoEndpoint());
        app.get("/ca.crt", new DownloadCaEndpoint());

        List<String> provisionerNames = List.of("prod", "testing");
        for (String provisionerName : provisionerNames) {


            String prefix = "/" + provisionerName;
            String crlLocation = "/crl/" + provisionerName + ".crl";

            Provisioner provisioner = new Provisioner(provisionerName, intermediateCertificate);


            app.get(crlLocation, new CRLEndpoint(provisioner, crlGenerator));

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
            app.post(prefix + "/acme/chall/{challengeId}", new ChallengeCallbackEndpoint(provisioner));

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

        log.info("Configure Routes completed. Ready for incoming requests");
    }


    private static void printBanner() {
        System.out.println("    _                       ____                           \n" +
                "   / \\   ___ _ __ ___   ___/ ___|  ___ _ ____   _____ _ __ \n" +
                "  / _ \\ / __| '_ ` _ \\ / _ \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|\n" +
                " / ___ \\ (__| | | | | |  __/___) |  __/ |   \\ V /  __/ |   \n" +
                "/_/   \\_\\___|_| |_| |_|\\___|____/ \\___|_|    \\_/ \\___|_|   \n");
    }

    private static void ensureFilesDirectoryExists() throws IOException {
        if (!Files.exists(FILES_DIR)) {
            log.info("First run detected, creating settings directory");
            Files.createDirectories(FILES_DIR);
        }
        Path configPath = FILES_DIR.resolve("settings.properties");
        if (!Files.exists(configPath)) {
            log.fatal("No configuration was found. Please create a file called \"settings.properties\" in \"" + FILES_DIR.toAbsolutePath() + "\". Then try again");
            System.exit(1);
        }
    }

    private static void loadConfiguration(Path configPath) throws IOException {
        log.info("Loading settings config into memory");
        properties.load(Files.newInputStream(configPath));
        log.info("Apply settings config");
        //Set DNS Name of this Server
        acmeThisServerDNSName = properties.getProperty("acme.server.dnsname");
        // Set Intermediate CA Settings
        intermediateCommonName = properties.getProperty("acme.certificate.intermediate.metadata.cn");
        caRSAKeyPairSize = Integer.parseInt(properties.getProperty("acme.certificate.intermediate.rsasize"));
        intermediateKeyStorePath = FILES_DIR.resolve(properties.getProperty("acme.certificate.intermediate.keystore.filename"));
        intermediateKeyStorePassword = properties.getProperty("acme.certificate.intermediate.keystore.password");
        intermediateKeyStoreAlias = properties.getProperty("acme.certificate.intermediate.keystore.alias");
        intermediateDefaultExpireDays = Integer.parseInt(properties.getProperty("acme.certificate.intermediate.expire.days"));
        intermediateDefaultExpireMonths = Integer.parseInt(properties.getProperty("acme.certificate.intermediate.expire.months"));
        intermediateDefaultExpireYears = Integer.parseInt(properties.getProperty("acme.certificate.intermediate.expire.years"));
        // ACME API Metadata
        acmeMetaWebsite = properties.getProperty("acme.api.meta.website");
        acmeMetaTermsOfService = properties.getProperty("acme.api.meta.termsofservice");
        // ACME API Port
        acmeThisServerAPIPort = Integer.parseInt(properties.getProperty("acme.server.sslport"));
        // Database Settings
        db_password = properties.getProperty("database.password");
        db_user = properties.getProperty("database.user");
        db_name = properties.getProperty("database.name");
        db_host = properties.getProperty("database.host");
        db_engine = properties.getProperty("database.engine");
        // Root CA Settings
        caCommonName = properties.getProperty("acme.certificate.root.metadata.cn");
        caRSAKeyPairSize = Integer.parseInt(properties.getProperty("acme.certificate.root.rsasize"));
        caPath = FILES_DIR.resolve(properties.getProperty("acme.certificate.root.certfile"));
        caKeyStorePassword = properties.getProperty("acme.certificate.root.keystore.password");
        caKeyStoreAlias = properties.getProperty("acme.certificate.root.keystore.alias");
        caDefaultExpireYears = Integer.parseInt(properties.getProperty("acme.certificate.root.expire.years"));
        caKeyStorePath = FILES_DIR.resolve(properties.getProperty("acme.certificate.root.keystore.filename"));
        // ACME API Server Settings
        acmeServerKeyStorePassword = properties.getProperty("acme.api.keystore.password");
        acmeServerKeyStorePath = FILES_DIR.resolve(properties.getProperty("acme.api.keystore.filename"));
        acmeServerRSAKeyPairSize = Integer.parseInt(properties.getProperty("acme.api.rsakeysize"));
        // E-Mail Settings
        emailSMTPPort = Integer.parseInt(properties.getProperty("email.smtp.port"));
        emailSMTPUsername = properties.getProperty("email.smtp.username");
        emailSMTPPassword = properties.getProperty("email.smtp.password");
        emailSMTPServer = properties.getProperty("email.smtp.server");
        emailSMTPEncryption = properties.getProperty("email.smtp.encryption");
        acmeAdminEmail = properties.getProperty("email.targetemail");
        // ACME created Certificates
        acmeCertificatesExpireDays = Integer.parseInt(properties.getProperty("acme.clientcert.expire.days"));
        acmeCertificatesExpireMonths = Integer.parseInt(properties.getProperty("acme.clientcert.expire.months"));
        acmeCertificatesExpireYears = Integer.parseInt(properties.getProperty("acme.clientcert.expire.years"));
        log.info("Settings have been successfully loaded");
    }

    private static void loadBuildAndGitMetadata() {
        try {
            Properties buildMetadataProperties = new Properties();
            buildMetadataProperties.load(Main.class.getResourceAsStream("/build.properties"));
            buildMetadataVersion = buildMetadataProperties.getProperty("build.version");
            buildMetadataBuildTime = buildMetadataProperties.getProperty("build.date") + " UTC";
        } catch (Exception e) {
            log.error("Unable to load build metadata", e);
        }
        try {
            Properties gitMetadataProperties = new Properties();
            gitMetadataProperties.load(Main.class.getResourceAsStream("/git.properties"));
            buildMetadataGitCommit = gitMetadataProperties.getProperty("git.commit.id.full");
        } catch (Exception e) {
            log.error("Unable to load git metadata", e);
        }
    }

    private static void initializeDatabaseDrivers() throws ClassNotFoundException, SQLException {
        log.info("Loading MariaDB JDBC driver");
        Class.forName("org.mariadb.jdbc.Driver");
        log.info("Loading H2 JDBC driver");
        Class.forName("org.h2.Driver");
    }

    private static void initializeCA() throws NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyStoreException, OperatorCreationException {
        if (!Files.exists(caPath) || !Files.exists(caKeyStorePath)) {
            // Create CA
            log.info("Generating RSA " + caRSAKeyPairSize + "bit Key Pair for CA");
            KeyPair caKeyPair = CertTools.generateRSAKeyPair(caRSAKeyPairSize);
            log.info("Creating CA");
            caCertificateBytes = CertTools.generateCertificateAuthorityCertificate(caCommonName, caDefaultExpireYears, caKeyPair);

            // Dumping CA Certificate to HDD, so other clients can install it
            log.info("Writing CA to disk");
            Files.createFile(caPath);
            Files.write(FILES_DIR.resolve(caPath), CertTools.certificateToPEM(caCertificateBytes).getBytes());
            // Save CA in Keystore
            log.info("Writing CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(caKeyPair, caKeyStorePassword, caKeyStoreAlias, caCertificateBytes, caKeyStorePath);


            //(Old) Intermediate CA is no longer valid, due to new Root CA
            if (Files.exists(intermediateKeyStorePath)) {
                Files.delete(intermediateKeyStorePath);
            }
        }
        if (!Files.exists(intermediateKeyStorePath)) {
            log.info("Loading Root CA Keypair");
            caKeyPair = KeyStoreUtils.loadFromPKCS12(caKeyStorePath, caKeyStorePassword, caKeyStoreAlias).getKeyPair();
            caCertificateBytes = CertTools.getCertificateBytes(caPath, caKeyPair);


            // *****************************************
            // Create Intermediate Certificate
            log.info("Generating RSA " + intermediateRSAKeyPairSize + "bit Key Pair for Intermediate CA");
            intermediateKeyPair = CertTools.generateRSAKeyPair(intermediateRSAKeyPairSize);
            log.info("Creating Intermediate CA");
            intermediateCertificate = CertTools.createIntermediateCertificate(caKeyPair, caCertificateBytes, intermediateKeyPair, intermediateCommonName, intermediateDefaultExpireDays, intermediateDefaultExpireMonths, intermediateDefaultExpireYears);
            log.info("Writing Intermediate CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(intermediateKeyPair, intermediateKeyStorePassword, intermediateKeyStoreAlias, intermediateCertificate.getEncoded(), intermediateKeyStorePath);

            //Unset, we don't need it anymore. Why should we keep it in memory?
            log.debug("Unset Root CA information");
            caKeyPair = null;
            caKeyStorePassword = null;
            caRSAKeyPairSize = 0;
            caKeyStoreAlias = null;

            //(Old) ACME API Server Certificate is no longer valid, due to new Intermediate CA
            if (Files.exists(acmeServerKeyStorePath)) {
                Files.delete(acmeServerKeyStorePath);
            }
        }


        if (!Files.exists(acmeServerKeyStorePath)) {
            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)
            log.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
            KeyPair acmeAPIKeyPair = CertTools.generateRSAKeyPair(4096);
            log.info("Creating Server Certificate");
            X509Certificate acmeAPICertificate = CertTools.createServerCertificate(intermediateKeyPair, intermediateCertificate.getEncoded(), acmeAPIKeyPair.getPublic().getEncoded(), new String[]{acmeThisServerDNSName}, 0, 1, 0);
            log.info("Writing Server Certificate (as key chain) to Key Store");
            KeyStoreUtils.saveAsPKCS12KeyChain(acmeAPIKeyPair, acmeServerKeyStorePassword, "server", new byte[][]{acmeAPICertificate.getEncoded(), intermediateCertificate.getEncoded()}, acmeServerKeyStorePath);

        }
    }

}