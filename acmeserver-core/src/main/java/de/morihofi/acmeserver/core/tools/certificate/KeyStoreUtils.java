/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.tools.certificate;

import de.morihofi.acmeserver.core.certificate.objects.KeyStoreFileContent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * Utility class for performing operations related to KeyStore such as saving and loading KeyPairs and certificates.
 */
public class KeyStoreUtils {

    /**
     * PKCS#12 type constant
     */
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
    public static void saveAsPKCS12KeyChain(KeyPair keyPair, String password, String alias, byte[][] certificates,
                                            Path targetLocation) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        char[] keystorePassword = password.toCharArray();

        ArrayList<X509Certificate> chain = new ArrayList<>(); // The certificate chain to be saved
        for (byte[] certificate : certificates) {
            chain.add(X509.convertToX509Cert(certificate));
        }

        try (OutputStream os = Files.newOutputStream(targetLocation)) {
            X509Certificate[] certificateChain = chain.toArray(new X509Certificate[0]);
            KeyStore keyStore = KeyStore.getInstance(PKCS12_INSTANCE_NAME);
            keyStore.load(null, keystorePassword); // Create a new, empty KeyStore
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), keystorePassword, certificateChain);
            keyStore.store(os, keystorePassword);
        }
    }

    /**
     * Loads a KeyPair and X.509 certificate from a PKCS12 keystore file.
     *
     * @param keyStorePath     The path to the PKCS12 keystore file.
     * @param keyStorePassword The password for the keystore.
     * @param keyAlias         The alias of the key pair to load.
     * @return A KeyStoreFileContent object containing the KeyPair and X.509 certificate.
     * @throws IOException               If there is an issue reading the keystore file.
     * @throws KeyStoreException         If there is a problem with the keystore.
     * @throws CertificateException      If there is an issue with the certificate.
     * @throws NoSuchAlgorithmException  If the algorithm required for the keystore is not available.
     * @throws UnrecoverableKeyException If the key cannot be recovered.
     */
    public static KeyStoreFileContent loadFromPKCS12(Path keyStorePath, String keyStorePassword, String keyAlias) throws IOException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        try (InputStream is = Files.newInputStream(keyStorePath)) {
            // Load the KeyPair and certificate from a PKCS12 keystore
            char[] keyStorePasswordCharArr = keyStorePassword.toCharArray();
            KeyStore keyStore = KeyStore.getInstance(PKCS12_INSTANCE_NAME);
            keyStore.load(is, keyStorePasswordCharArr);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePasswordCharArr);
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(keyAlias);
            KeyPair keyPair = new KeyPair(cert.getPublicKey(), privateKey);

            return new KeyStoreFileContent(keyPair, cert, keyAlias);
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private KeyStoreUtils() {}
}
