package de.morihofi.acmeserver;

import de.morihofi.acmeserver.certificate.CertTools;
import de.morihofi.acmeserver.certificate.KeyStoreUtils;
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

    static Logger log = LogManager.getLogger(Main.class);

    static KeyPair intermediateKeyPair;
    static X509Certificate intermediateCertificate;

    public static void main(String[] args) throws Exception {

        Path filesDir = Paths.get("serverdata").toAbsolutePath();

        Path acmeServerKeyStorePath = filesDir.resolve("acmeserver.p12");
        String acmeServerKeyStorePassword = "";

        Path intermediateKeyStorePath = filesDir.resolve("intermediate.p12");

        //Register Bouncy Castle Provider
        log.info("Register Bouncy Castle Security Provider");
        Security.addProvider(new BouncyCastleProvider());


        //Detect first run
        System.out.println(filesDir);

        /*
        try {
            Files.walk(filesDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        }catch (Exception ex){

        }
*/

        if (!Files.exists(filesDir)) {
            log.info("First run detected, settings things up");

            Files.createDirectories(filesDir);

            // Create CA
            log.info("Generating RSA Key Pair for CA");
            KeyPair caKeyPair = CertTools.generateRSAKeyPair(4096);
            log.info("Creating CA");
            byte[] caCertificateBytes = CertTools.generateCertificateAuthorityCertificate("MHO Test CA 1", 100, caKeyPair);

            // Dumping CA Certificate to HDD, so other clients can install it
            log.info("Writing CA to disk");
            Path caPath = filesDir.resolve("ca.crt");
            Files.createFile(caPath);
            Files.write(filesDir.resolve(caPath), CertTools.certificateToPEM(caCertificateBytes).getBytes());
            // Save CA in Keystore
            log.info("Writing CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(caKeyPair, "", "ca", caCertificateBytes, filesDir.resolve("ca.p12"));



            // *****************************************
            // Create Intermediate Certificate
            log.info("Generating RSA Key Pair for Intermediate CA");
            intermediateKeyPair = CertTools.generateRSAKeyPair(4096);
            log.info("Creating Intermediate CA");
            intermediateCertificate = CertTools.createIntermediateCertificate(caKeyPair,caCertificateBytes,intermediateKeyPair,"MHO Test CA Intermediate",30,0,0);
            log.info("Writing Intermediate CA to Key Store");
            KeyStoreUtils.saveAsPKCS12(intermediateKeyPair, "", "intermediate", intermediateCertificate.getEncoded(), intermediateKeyStorePath);

            //Unset, we doesn't need it anymore. Why should we keep it in memory?#
            log.debug("Unset CA Key Pair");
            caKeyPair = null;

            // *****************************************
            // Create Certificate for our ACME Web Server API (Client Certificate)
            log.info("Generating RSA Key Pair for ACME Web Server API");
            KeyPair acmeAPIKeyPair = CertTools.generateRSAKeyPair(4096);
            log.info("Creating Server Certificate");
            X509Certificate acmeAPICertificate = CertTools.createServerCertificate(intermediateKeyPair,intermediateCertificate.getEncoded(), acmeAPIKeyPair.getPublic().getEncoded(), new String[]{"example.com","www.example.com","git.example.com"},0,1,0);
            log.info("Writing Server Certificate to Key Store");
            KeyStoreUtils.saveAsPKCS12(acmeAPIKeyPair, acmeServerKeyStorePassword, "server", acmeAPICertificate.getEncoded(), acmeServerKeyStorePath);



        } else {
            log.info("Loading certificates");
            KeyStoreFileContent keyStoreContents = KeyStoreUtils.loadFromPKCS12(intermediateKeyStorePath,acmeServerKeyStorePassword, "intermediate");
            log.info("Loading intermediate Key Pair");
            intermediateKeyPair =keyStoreContents.getKeyPair();
            log.info("Loading intermediate Certificate");
            intermediateCertificate = keyStoreContents.getCert();
        }

        log.info("Starting API WebServer using HTTPS");
        String keyStoreLocation = acmeServerKeyStorePath.toAbsolutePath().toString();
        String keyStorePassword = "";
        Spark.secure(keyStoreLocation, keyStorePassword, null, null);
        Spark.port(7443);
        Spark.get("/hello", (req, res) -> "Hello World");



    }
}