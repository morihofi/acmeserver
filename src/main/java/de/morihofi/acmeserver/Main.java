package de.morihofi.acmeserver;

import de.morihofi.acmeserver.certificate.CertTools;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Comparator;

public class Main {

    static KeyPair caKeyPair;
    static byte[] caCertificateBytes;

    static KeyPair intermediateKeyPair;

    public static void main(String[] args) throws Exception {

        Path filesDir = Paths.get("serverdata").toAbsolutePath();

        //Register Bouncy Castle Provider
        Security.addProvider(new BouncyCastleProvider());

        //Detect first run
        System.out.println(filesDir);

        Files.walk(filesDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        if (!Files.exists(filesDir)) {


            Files.createDirectories(filesDir);

            // Create CA
            caKeyPair = CertTools.generateRSA(4096);
            caCertificateBytes = CertTools.generateCertificateAuthorityCertificate("MHO Test CA 1", 100, caKeyPair);
            // Dumping CA Certificate to HDD, so other clients can install it
            Path caPath = filesDir.resolve("ca.crt");
            Files.createFile(caPath);
            Files.write(filesDir.resolve(caPath), CertTools.certificateToPEM(caCertificateBytes).getBytes());
            // Save Keystore
            CertTools.saveAsPKCS12(caKeyPair, "", "alias", caCertificateBytes, filesDir.resolve("ca.p12"));

            // *****************************************
            // Create Intermediate Certificate
            intermediateKeyPair = CertTools.generateRSA(4096);
            X509Certificate intermediateCertificate = CertTools.createIntermediateCertificate(caKeyPair,caCertificateBytes,intermediateKeyPair,"MHO Test CA Intermediate",30,0,0);
            CertTools.saveAsPKCS12(intermediateKeyPair, "", "intermediate", intermediateCertificate.getEncoded(), filesDir.resolve("intermediate.p12"));


            System.out.println(CertTools.certificateToPEM(caCertificateBytes));
        } else {

            System.out.println("TODO: Load already created certificates");
        }


        Spark.port(9443);


    }
}