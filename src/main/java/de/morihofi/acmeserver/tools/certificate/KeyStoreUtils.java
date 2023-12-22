package de.morihofi.acmeserver.tools.certificate;

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

    private KeyStoreUtils(){}

    public static final Logger log = LogManager.getLogger(KeyStoreUtils.class);
    private static final String PKCS12_INSTANCE_NAME = "PKCS12";


    /**
     * Saves a KeyPair and X.509 certificate as a PKCS12 keystore.
     *
     * @param keyPair        The KeyPair to save.
     * @param password       The password to protect the keystore.
     * @param alias          The alias for the KeyPair within the keystore.
     * @param certificate    The X.509 certificate to save.
     * @param targetLocation The path to save the PKCS12 keystore.
     * @throws KeyStoreException        If there is an issue with the keystore.
     * @throws CertificateException     If there is an issue with the certificate.
     * @throws IOException              If there is an issue reading or writing the keystore file.
     * @throws NoSuchAlgorithmException If the algorithm required for the keystore is not available.
     */
    public static void saveAsPKCS12(KeyPair keyPair, String password, String alias, byte[] certificate, Path targetLocation)
            throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        // Convert the password to a char array
        char[] passwordCharArr = password.toCharArray();

        // Create or load a KeyStore object
        KeyStore keyStore = KeyStore.getInstance(PKCS12_INSTANCE_NAME);
        if (Files.exists(targetLocation)) {
            // If the file exists, load the existing KeyStore
            try (InputStream is = Files.newInputStream(targetLocation)) {
                keyStore.load(is, passwordCharArr);
            }
        } else {
            // Otherwise, initialize a new KeyStore
            keyStore.load(null, passwordCharArr);
        }

        // Add the KeyPair and certificate to the KeyStore
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), passwordCharArr, new X509Certificate[]{X509.convertToX509Cert(certificate)});

        // Save the KeyStore object as a PKCS12 file
        try (OutputStream fos = Files.newOutputStream(targetLocation)) {
            keyStore.store(fos, passwordCharArr);
        }
    }

    /**
     * Saves a KeyPair and X.509 certificate chain as a PKCS12 keystore.
     *
     * @param keyPair        The KeyPair to save.
     * @param password       The password to protect the keystore.
     * @param alias          The alias for the KeyPair within the keystore.
     * @param certificates   The X.509 certificate chain to save.
     * @param targetLocation The path to save the PKCS12 keystore.
     * @throws KeyStoreException        If there is an issue with the keystore.
     * @throws IOException              If there is an issue writing the keystore file.
     * @throws CertificateException     If there is an issue with the certificates.
     * @throws NoSuchAlgorithmException If the algorithm required for the keystore is not available.
     */
    public static void saveAsPKCS12KeyChain(KeyPair keyPair, String password, String alias, byte[][] certificates, Path targetLocation) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        char[] keystorePassword = password.toCharArray();

        ArrayList<X509Certificate> chain = new ArrayList<>(); // die Zertifikatskette, die gespeichert werden soll
        for (byte[] certificate : certificates) {
            chain.add(X509.convertToX509Cert(certificate));
        }

        try (OutputStream os = Files.newOutputStream(targetLocation)) {
            X509Certificate[] certificateChain = chain.toArray(new X509Certificate[0]);
            KeyStore keyStore = KeyStore.getInstance(PKCS12_INSTANCE_NAME);
            keyStore.load(null, keystorePassword); // Neuen, leeren KeyStore erstellen
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), keystorePassword, certificateChain);
            keyStore.store(os, keystorePassword);
        }


    }


    /**
     * Loads a KeyPair and X.509 certificate from a PKCS12 keystore file.
     *
     * @param keyStorePath     The path to the PKCS12 keystore file.
     * @param keyStorePassword The password for the keystore.
     * @return A KeyStoreFileContent object containing the KeyPair and X.509 certificate.
     * @throws IOException               If there is an issue reading the keystore file.
     * @throws KeyStoreException         If there is a problem with the keystore.
     * @throws CertificateException      If there is an issue with the certificate.
     * @throws NoSuchAlgorithmException  If the algorithm required for the keystore is not available.
     * @throws UnrecoverableKeyException If the key cannot be recovered.
     */
    public static KeyStoreFileContent loadFromPKCS12(Path keyStorePath, String keyStorePassword, String keyAlias) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        try (InputStream is = Files.newInputStream(keyStorePath)) {
            // Laden des KeyPairs und des Zertifikats aus einem PKCS12-Keystore
            char[] keyStorePasswordCharArr = keyStorePassword.toCharArray();
            KeyStore keyStore = KeyStore.getInstance(PKCS12_INSTANCE_NAME);
            keyStore.load(is, keyStorePasswordCharArr);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePasswordCharArr);
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);
            KeyPair keyPair = new KeyPair(cert.getPublicKey(), privateKey);

            return new KeyStoreFileContent(keyPair, cert, keyAlias);
        }
    }

}
