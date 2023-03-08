package de.morihofi.acmeserver.certificate.tools;

import de.morihofi.acmeserver.certificate.objects.KeyStoreFileContent;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

public class KeyStoreUtils {
    public static void saveAsPKCS12(KeyPair keyPair, String password, String alias, byte[] certificate, Path targetLocation) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        // Erzeuge ein KeyStore-Objekt und füge das KeyPair und das Zertifikat hinzu
        char[] passwordCharArr = password.toCharArray();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, passwordCharArr);


        keyStore.setKeyEntry(alias, keyPair.getPrivate(), passwordCharArr, new X509Certificate[]{CertTools.convertToX509Cert(certificate)});


        // Speichere das KeyStore-Objekt als PKCS12-Datei
        OutputStream fos = Files.newOutputStream(targetLocation);
        keyStore.store(fos, passwordCharArr);
        fos.close();
    }

    public static void saveAsPKCS12KeyChain(KeyPair keyPair, String password, String alias, byte[][] certificates, Path targetLocation) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        char[] keystorePassword = password.toCharArray();
        String keyAlias = alias;

        ArrayList<X509Certificate> chain = new ArrayList(); // die Zertifikatskette, die gespeichert werden soll
        for (byte[] certificate : certificates) {
            chain.add(CertTools.convertToX509Cert(certificate));
        }


        X509Certificate[] certificateChain = chain.toArray(new X509Certificate[0]);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, keystorePassword); // Neuen, leeren KeyStore erstellen
        keyStore.setKeyEntry(keyAlias, (Key) keyPair.getPrivate(), keystorePassword, certificateChain);
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
            throw new RuntimeException(e);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        try {
            dos = new DataOutputStream(Files.newOutputStream(targetDirectory.resolve("rsaPrivateKey")));
            dos.write(privateKey.getEncoded());
            dos.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (dos != null)
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static KeyPair openRSAKeyPairFromDirectory(Path targetDirectory) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(getFileBytes(targetDirectory.resolve("rsaPrivateKey")));
        KeyFactory privateKf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = privateKf.generatePrivate(privateSpec);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(getFileBytes(targetDirectory.resolve("rsaPublicKey")));
        KeyFactory publicKf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = publicKf.generatePublic(spec);

        KeyPair kp = new KeyPair(publicKey,privateKey);

        return kp;
    }

    private static byte[] getFileBytes(Path fileLocation) throws IOException {
        DataInputStream dis = new DataInputStream(Files.newInputStream(fileLocation));
        byte[] fileBytes = new byte[(int) Files.size(fileLocation)];
        dis.readFully(fileBytes);
        dis.close();

        return fileBytes;
    }


}
