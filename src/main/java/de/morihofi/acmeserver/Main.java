package de.morihofi.acmeserver;

import de.morihofi.acmeserver.certificate.Database;
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
import java.util.Comparator;

public class Main {

    public static Logger log = LogManager.getLogger(Main.class);

    public static KeyPair intermediateKeyPair;
    public static X509Certificate intermediateCertificate;

    static enum STARTUP_MODE {
        SERVER, FIRSTRUN
    }

    public static Path filesDir = Paths.get("serverdata").toAbsolutePath();

    //ACME SERVER Certificate
    public static Path acmeServerKeyStorePath = filesDir.resolve("acmeserver.p12");
    public static String acmeServerKeyStorePassword = "";

    //CA Certificate
    public static String caKeyStorePassword = "";
    public static String caCommonName = "MHO Test CA 1 - Do not use in Production";
    public static int caRSAKeyPairSize = 4096;
    public static Path caPath;

    //Intermediate CA Certificate
    public static Path intermediateKeyStorePath = filesDir.resolve("intermediate.p12");
    public static String intermediateKeyStorePassword = "";
    public static String intermediateCommonName = "MHO Test CA 1 - Intermediate - Do not use in Production";
    public static int intermediateRSAKeyPairSize = 4096;

    //ACME Directory Information
    public static String acmeMetaWebsite = "https://morihofi.de";
    public static String acmeMetaTermsOfService = "https://morihofi.de/tos.php";
    public static String acmeThisServerDNSName = "mo-nb-gb-mint"; // "mho-nb.hq.ifd-gmbh.com";
    public static int acmeThisServerAPIPort = 7443;

    //ACME Signature
    public static KeyPair acmeSignatureKeyPair;
    public static Path acmeSignatureKeyPairPath = filesDir.resolve("acme-signature");


    public static void main(String[] args) throws Exception {

        System.out.println("    _                       ____                           \n" +
                "   / \\   ___ _ __ ___   ___/ ___|  ___ _ ____   _____ _ __ \n" +
                "  / _ \\ / __| '_ ` _ \\ / _ \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|\n" +
                " / ___ \\ (__| | | | | |  __/___) |  __/ |   \\ V /  __/ |   \n" +
                "/_/   \\_\\___|_| |_| |_|\\___|____/ \\___|_|    \\_/ \\___|_|   \n");


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
        caPath = filesDir.resolve("ca.crt");

        //Detect first run
        if (!Files.exists(filesDir)) {
            log.info("First run detected, settings things up");
            Files.createDirectories(filesDir);

            // Create CA
            log.info("Generating RSA " + caRSAKeyPairSize + "bit Key Pair for CA");
            KeyPair caKeyPair = CertTools.generateRSAKeyPair(caRSAKeyPairSize);
            log.info("Creating CA");
            byte[] caCertificateBytes = CertTools.generateCertificateAuthorityCertificate(caCommonName, 100, caKeyPair);

            // Dumping CA Certificate to HDD, so other clients can install it
            log.info("Writing CA to disk");
            Files.createFile(caPath);
            Files.write(filesDir.resolve(caPath), CertTools.certificateToPEM(caCertificateBytes).getBytes());
            // Save CA in Keystore
            log.info("Writing CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(caKeyPair, caKeyStorePassword, "ca", caCertificateBytes, filesDir.resolve("ca.p12"));



            // *****************************************
            // Create Intermediate Certificate
            log.info("Generating RSA " + intermediateRSAKeyPairSize + "bit Key Pair for Intermediate CA");
            intermediateKeyPair = CertTools.generateRSAKeyPair(intermediateRSAKeyPairSize);
            log.info("Creating Intermediate CA");
            intermediateCertificate = CertTools.createIntermediateCertificate(caKeyPair,caCertificateBytes,intermediateKeyPair,intermediateCommonName,90,0,0);
            log.info("Writing Intermediate CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(intermediateKeyPair, intermediateKeyStorePassword, "intermediate", intermediateCertificate.getEncoded(), intermediateKeyStorePath);

            //Unset, we don't need it anymore. Why should we keep it in memory?#
            log.debug("Unset CA Key Pair");
            caKeyPair = null;

            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)
            log.info("Generating RSA Key Pair for ACME Web Server API (HTTPS Service)");
            KeyPair acmeAPIKeyPair = CertTools.generateRSAKeyPair(4096);
            log.info("Creating Server Certificate");
            X509Certificate acmeAPICertificate = CertTools.createServerCertificate(intermediateKeyPair,intermediateCertificate.getEncoded(), acmeAPIKeyPair.getPublic().getEncoded(), new String[]{"example.com","www.example.com","git.example.com", acmeThisServerDNSName},0,1,0);
            log.info("Writing Server Certificate (as key chain) to Key Store");
            KeyStoreUtils.saveAsPKCS12KeyChain(acmeAPIKeyPair, acmeServerKeyStorePassword, "server", new byte[][]{acmeAPICertificate.getEncoded(), intermediateCertificate.getEncoded()}, acmeServerKeyStorePath);


            // *****************************************
            log.info("Generating RSA Keypair for ACME Signature");
            acmeSignatureKeyPair = CertTools.generateRSAKeyPair(4096);
            log.info("Saving ACME Signature Keypair to Disk");

            KeyStoreUtils.saveRSAKeyPairToDirectory(acmeSignatureKeyPair,acmeSignatureKeyPairPath);


        } else {
            log.info("Loading certificates");
            KeyStoreFileContent keyStoreContents = KeyStoreUtils.loadFromPKCS12(intermediateKeyStorePath,acmeServerKeyStorePassword, "intermediate");
            log.info("Loading intermediate Key Pair");
            intermediateKeyPair =keyStoreContents.getKeyPair();
            log.info("Loading intermediate Certificate");
            intermediateCertificate = keyStoreContents.getCert();

            log.info("Loading ACME Signature Keypair");
            acmeSignatureKeyPair = KeyStoreUtils.openRSAKeyPairFromDirectory(acmeSignatureKeyPairPath);
        }


        log.info("Starting ACME API WebServer using HTTPS");
        String keyStoreLocation = acmeServerKeyStorePath.toAbsolutePath().toString();
        String keyStorePassword = "";
        Spark.secure(keyStoreLocation, keyStorePassword, null, null);
        Spark.port(acmeThisServerAPIPort);
        Spark.staticFileLocation("/webstatic/gethttpsforfree");

        log.info("Configure Routes");
        Spark.before((request, response) -> {
           response.header("Access-Control-Allow-Origin","*");
           response.header("Access-Control-Allow-Methods","*");

           log.info("API Call [" + request.requestMethod() + "] " + request.raw().getPathInfo());
        });

        // TODO: Download CA Endpoint
        //Spark.get("/ca.pem", AcmeAPI.downloadCA);

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


    }
}