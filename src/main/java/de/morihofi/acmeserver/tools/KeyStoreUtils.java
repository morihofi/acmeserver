package de.morihofi.acmeserver.tools;

import de.morihofi.acmeserver.certificate.objects.KeyStoreFileContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

public class KeyStoreUtils {

    public static final Logger log = LogManager.getLogger(KeyStoreUtils.class);


    public static void saveAsPKCS12(KeyPair keyPair, String password, String alias, byte[] certificate, Path targetLocation) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        // Konvertiere das Passwort in ein Char-Array
        char[] passwordCharArr = password.toCharArray();

        // Erstelle oder lade ein KeyStore-Objekt
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        if (Files.exists(targetLocation)) {
            // Wenn die Datei existiert, lade das vorhandene KeyStore
            try (InputStream is = Files.newInputStream(targetLocation)) {
                keyStore.load(is, passwordCharArr);
            }
        } else {
            // Andernfalls initialisiere ein neues KeyStore
            keyStore.load(null, passwordCharArr);
        }

        // Füge das KeyPair und das Zertifikat hinzu
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), passwordCharArr, new X509Certificate[]{CertTools.convertToX509Cert(certificate)});

        // Speichere das KeyStore-Objekt als PKCS12-Datei
        try (OutputStream fos = Files.newOutputStream(targetLocation)) {
            keyStore.store(fos, passwordCharArr);
        }
    }

    public static void saveAsPKCS12KeyChain(KeyPair keyPair, String password, String alias, byte[][] certificates, Path targetLocation) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        char[] keystorePassword = password.toCharArray();
        String keyAlias = alias;

        ArrayList<X509Certificate> chain = new ArrayList<>(); // die Zertifikatskette, die gespeichert werden soll
        for (byte[] certificate : certificates) {
            chain.add(CertTools.convertToX509Cert(certificate));
        }


        X509Certificate[] certificateChain = chain.toArray(new X509Certificate[0]);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, keystorePassword); // Neuen, leeren KeyStore erstellen
        keyStore.setKeyEntry(keyAlias, keyPair.getPrivate(), keystorePassword, certificateChain);
        keyStore.store(Files.newOutputStream(targetLocation), keystorePassword);
    }

    public static KeyStoreFileContent loadFromPKCS12(Path keyStorePath, String keyStorePassword, String certificateAlias) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        // Laden des KeyPairs und des Zertifikats aus einem PKCS12-Keystore
        char[] keyStorePasswordCharArr = keyStorePassword.toCharArray();
        String keyAlias = certificateAlias;
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(Files.newInputStream(keyStorePath), keyStorePasswordCharArr);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePasswordCharArr);
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);
        KeyPair keyPair = new KeyPair(cert.getPublicKey(), privateKey);

        return new KeyStoreFileContent(keyPair, cert, certificateAlias);
    }

    public static void saveRSAKeyPairToDirectory(KeyPair keyPair, Path targetDirectory) throws IOException {
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        if (!Files.exists(targetDirectory)){
            Files.createDirectories(targetDirectory);
        }

        Files.createFile(targetDirectory.resolve("rsaPublicKey"));
        Files.createFile(targetDirectory.resolve("rsaPrivateKey"));


        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(Files.newOutputStream(targetDirectory.resolve("rsaPublicKey")));
            dos.write(publicKey.getEncoded());
            dos.flush();
        } catch (Exception e) {
            log.error("An exception was thrown",e);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    log.error("Unable to close DataOutputStream",e);
                }
            }

        }

        try {
            dos = new DataOutputStream(Files.newOutputStream(targetDirectory.resolve("rsaPrivateKey")));
            dos.write(privateKey.getEncoded());
            dos.flush();
        } catch (Exception e) {
            log.error("An exception was thrown",e);
        } finally {
            if (dos != null)
                try {
                    dos.close();
                } catch (IOException e) {
                    log.error("Unable to close DataOutputStream",e);
                }
        }
    }


}
