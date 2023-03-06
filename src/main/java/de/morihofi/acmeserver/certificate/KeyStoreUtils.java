package de.morihofi.acmeserver.certificate;

import de.morihofi.acmeserver.certificate.objects.KeyStoreFileContent;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

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
        for (byte[] certificate: certificates) {
            chain.add(CertTools.convertToX509Cert(certificate));
        }


        X509Certificate[] certificateChain = chain.toArray(new X509Certificate[0]);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, keystorePassword); // Neuen, leeren KeyStore erstellen
        keyStore.setKeyEntry(keyAlias, (Key) keyPair.getPrivate(), keystorePassword, certificateChain);
        keyStore.store(Files.newOutputStream(targetLocation), keystorePassword);
    }

    public static KeyStoreFileContent loadFromPKCS12(Path keyStorePath,String keyStorePassword, String certificateAlias) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        // Laden des KeyPairs und des Zertifikats aus einem PKCS12-Keystore
        char[] keyStorePasswordCharArr = keyStorePassword.toCharArray();
        String keyAlias = certificateAlias;
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(Files.newInputStream(keyStorePath), keyStorePasswordCharArr);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePasswordCharArr);
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);
        KeyPair keyPair = new KeyPair(cert.getPublicKey(), privateKey);

        return new KeyStoreFileContent(keyPair,cert,certificateAlias);
    }

}
