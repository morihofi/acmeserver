package de.morihofi.acmeserver;

import de.morihofi.acmeserver.certificate.Database;
import de.morihofi.acmeserver.certificate.JWSTestSignExample;
import de.morihofi.acmeserver.certificate.SendMail;
import de.morihofi.acmeserver.certificate.tools.Base64Tools;
import de.morihofi.acmeserver.certificate.tools.CertTools;
import de.morihofi.acmeserver.certificate.tools.KeyStoreUtils;
import de.morihofi.acmeserver.certificate.acmeapi.AcmeAPI;
import de.morihofi.acmeserver.certificate.objects.KeyStoreFileContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import spark.Spark;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Comparator;
import java.util.Properties;

public class Main {

    public static Logger log = LogManager.getLogger(Main.class);

    public static KeyPair intermediateKeyPair;
    public static X509Certificate intermediateCertificate;

    // Database
    public static String db_password;
    public static String db_user;
    public static String db_name;
    public static String db_host;

    static enum STARTUP_MODE {
        SERVER, FIRSTRUN
    }

    public static Path filesDir = Paths.get("serverdata").toAbsolutePath();

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

    //ACME Signature
    public static KeyPair acmeSignatureKeyPair;
    public static Path acmeSignatureKeyPairPath = filesDir.resolve("acme-signature");

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

    public static void main(String[] args) throws Exception {

        System.out.println("    _                       ____                           \n" +
                "   / \\   ___ _ __ ___   ___/ ___|  ___ _ ____   _____ _ __ \n" +
                "  / _ \\ / __| '_ ` _ \\ / _ \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|\n" +
                " / ___ \\ (__| | | | | |  __/___) |  __/ |   \\ V /  __/ |   \n" +
                "/_/   \\_\\___|_| |_| |_|\\___|____/ \\___|_|    \\_/ \\___|_|   \n");


        //Detect first run
        if (!Files.exists(filesDir)) {
            log.info("First run detected, creating settings directory");
            Files.createDirectories(filesDir);
        }
        Path configPath = filesDir.resolve("settings.properties");
        if (!Files.exists(configPath)) {
            log.fatal("No configuration was found. Please create a file called \"settings.properties\" in \"" + filesDir.toAbsolutePath().toString() + "\". Then try again");
            System.exit(1);
        }


        log.info("Loading settings config into memory");
        properties.load(Files.newInputStream(configPath));
        log.info("Apply settings config");
        //Set DNS Name of this Server
        acmeThisServerDNSName = properties.getProperty("acme.server.dnsname");
        // Set Intermediate CA Settings
        intermediateCommonName = properties.getProperty("acme.certificate.intermediate.metadata.cn");
        caRSAKeyPairSize = Integer.parseInt(properties.getProperty("acme.certificate.intermediate.rsasize"));
        intermediateKeyStorePath = filesDir.resolve(properties.getProperty("acme.certificate.intermediate.keystore.filename"));
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
        // Root CA Settings
        caCommonName = properties.getProperty("acme.certificate.root.metadata.cn");
        caRSAKeyPairSize = Integer.parseInt(properties.getProperty("acme.certificate.root.rsasize"));
        caPath = filesDir.resolve(properties.getProperty("acme.certificate.root.certfile"));
        caKeyStorePassword = properties.getProperty("acme.certificate.root.keystore.password");
        caKeyStoreAlias = properties.getProperty("acme.certificate.root.keystore.alias");
        caDefaultExpireYears = Integer.parseInt(properties.getProperty("acme.certificate.root.expire.years"));
        caKeyStorePath = filesDir.resolve(properties.getProperty("acme.certificate.root.keystore.filename"));
        // ACME API Server Settings
        acmeServerKeyStorePassword = properties.getProperty("acme.api.keystore.password");
        acmeServerKeyStorePath = filesDir.resolve(properties.getProperty("acme.api.keystore.filename"));
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


        //Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Provider");
        Security.addProvider(new BouncyCastleProvider());

        //Loading MariaDB Database driver
        log.info("Loading MariaDB JDBC driver");
        Class.forName("org.mariadb.jdbc.Driver");

        String dbVersion = Database.getDatabaseVersion();
        log.info("Server uses Database: " + dbVersion);

/*
        try {
            Files.walk(filesDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        }catch (Exception ex){

        }
*/




        if(!Files.exists(caPath) || !Files.exists(caKeyStorePath)){
            // Create CA
            log.info("Generating RSA " + caRSAKeyPairSize + "bit Key Pair for CA");
            KeyPair caKeyPair = CertTools.generateRSAKeyPair(caRSAKeyPairSize);
            log.info("Creating CA");
            caCertificateBytes = CertTools.generateCertificateAuthorityCertificate(caCommonName, caDefaultExpireYears, caKeyPair);

            // Dumping CA Certificate to HDD, so other clients can install it
            log.info("Writing CA to disk");
            Files.createFile(caPath);
            Files.write(filesDir.resolve(caPath), CertTools.certificateToPEM(caCertificateBytes).getBytes());
            // Save CA in Keystore
            log.info("Writing CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(caKeyPair, caKeyStorePassword, caKeyStoreAlias, caCertificateBytes, caKeyStorePath);


            //(Old) Intermediate CA is no longer valid, due to new Root CA
            if (Files.exists(intermediateKeyStorePath)){
                Files.delete(intermediateKeyStorePath);
            }
        }
        if (!Files.exists(intermediateKeyStorePath)){
            log.info("Loading Root CA Keypair");
            caKeyPair = KeyStoreUtils.loadFromPKCS12(caKeyStorePath,caKeyStorePassword,caKeyStoreAlias).getKeyPair();
            String caPEM = new String(Files.readAllBytes(caPath));
            caPEM = caPEM.replaceAll("-----(BEGIN|END) CERTIFICATE-----", "").replaceAll("\n", "");
            caCertificateBytes = Base64.getDecoder().decode(caPEM.getBytes("UTF-8"));



            // *****************************************
            // Create Intermediate Certificate
            log.info("Generating RSA " + intermediateRSAKeyPairSize + "bit Key Pair for Intermediate CA");
            intermediateKeyPair = CertTools.generateRSAKeyPair(intermediateRSAKeyPairSize);
            log.info("Creating Intermediate CA");
            intermediateCertificate = CertTools.createIntermediateCertificate(caKeyPair,caCertificateBytes,intermediateKeyPair,intermediateCommonName,intermediateDefaultExpireDays,intermediateDefaultExpireMonths,intermediateDefaultExpireYears);
            log.info("Writing Intermediate CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(intermediateKeyPair, intermediateKeyStorePassword, intermediateKeyStoreAlias, intermediateCertificate.getEncoded(), intermediateKeyStorePath);

            //Unset, we don't need it anymore. Why should we keep it in memory?
            log.debug("Unset Root CA information");
            caKeyPair = null;
            caKeyStorePassword = null;
            caRSAKeyPairSize = 0;
            caKeyStoreAlias = null;

            //(Old) ACME API Server Certificate is no longer valid, due to new Intermediate CA
            if (Files.exists(acmeServerKeyStorePath)){
                Files.delete(acmeServerKeyStorePath);
            }
        }


        if (!Files.exists(acmeServerKeyStorePath)){
            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)
            log.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
            KeyPair acmeAPIKeyPair = CertTools.generateRSAKeyPair(4096);
            log.info("Creating Server Certificate");
            X509Certificate acmeAPICertificate = CertTools.createServerCertificate(intermediateKeyPair,intermediateCertificate.getEncoded(), acmeAPIKeyPair.getPublic().getEncoded(), new String[]{acmeThisServerDNSName},0,1,0);
            log.info("Writing Server Certificate (as key chain) to Key Store");
            KeyStoreUtils.saveAsPKCS12KeyChain(acmeAPIKeyPair, acmeServerKeyStorePassword, "server", new byte[][]{acmeAPICertificate.getEncoded(), intermediateCertificate.getEncoded()}, acmeServerKeyStorePath);


           // *****************************************
           log.info("Generating RSA Keypair for ACME Signature");
           acmeSignatureKeyPair = CertTools.generateRSAKeyPair(acmeServerRSAKeyPairSize);
           log.info("Saving ACME Signature Keypair to Disk");

           KeyStoreUtils.saveRSAKeyPairToDirectory(acmeSignatureKeyPair,acmeSignatureKeyPairPath);

       }






       log.info("Loading certificates");
       KeyStoreFileContent intermediateKeyStoreContents = KeyStoreUtils.loadFromPKCS12(intermediateKeyStorePath,intermediateKeyStorePassword, intermediateKeyStoreAlias);
       log.info("Loading intermediate Key Pair");
       intermediateKeyPair =intermediateKeyStoreContents.getKeyPair();
       log.info("Loading intermediate Certificate");
       intermediateCertificate = intermediateKeyStoreContents.getCert();



        log.info("Starting ACME API WebServer using HTTPS");
        String keyStoreLocation = acmeServerKeyStorePath.toAbsolutePath().toString();
        Spark.secure(keyStoreLocation, acmeServerKeyStorePassword, null, null);
        Spark.port(acmeThisServerAPIPort);
        Spark.staticFileLocation("/webstatic");

        log.info("Configure Routes");
        Spark.before((request, response) -> {
           response.header("Access-Control-Allow-Origin","*");
           response.header("Access-Control-Allow-Methods","*");

           log.info("API Call [" + request.requestMethod() + "] " + request.raw().getPathInfo());
        });

        // TODO: Download CA Endpoint
        Spark.get("/ca.crt", AcmeAPI.downloadCA);

        Spark.get("/directory", AcmeAPI.directoryEndpoint);

        // New account
        Spark.post("/acme/new-acct", AcmeAPI.newAccount);

        // New Nonce -> Supports (in the RFC) only HEAD (Status 200) and GET (Status 204)
        Spark.head("/acme/new-nonce", AcmeAPI.newNonce);
        Spark.get("/acme/new-nonce", AcmeAPI.newNonce);

        // Account Update
        Spark.post("/acme/acct/:id", AcmeAPI.account);

        // Create new Order
        Spark.post("/acme/new-order", AcmeAPI.newOrder);

        // Challenge / Ownership verification
        Spark.post("/acme/authz/:authorizationId", AcmeAPI.authz);
        // Challenge Callback
        Spark.post("/acme/chall/:challengeId", AcmeAPI.challengeCallback);
        // Finalize endpoint
        Spark.post("/acme/order/:orderId/finalize", AcmeAPI.finalizeOrder);
        // Order info Endpoint
        Spark.post("/acme/order/:orderId", AcmeAPI.order);
        // Get Order Certificate
        Spark.post("/acme/order/:orderId/cert", AcmeAPI.orderCert);

        log.info("Configure Routes completed. Ready for incoming requests");
    }
}