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

package de.morihofi.acmeserver.cryptography.keystore;


import lombok.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * A class handling KeyStore operations. Original source <a
 * href="https://github.com/gerritjvv/engsectools/blob/master/pkcs11/src/main/java/engsec/KeyStoreUtil.java">GitHub
 * gerritjvv/engsectools</a> and modified some methods.
 */
public class KeyStoreUtil {

    /**
     * Saves the KeyStore to the specified path.
     *
     * @param ks   The KeyStore to be saved.
     * @param path The path where the KeyStore will be saved.
     * @param pwd  The password for the KeyStore.
     */
    public static void saveKeyStore(@NonNull KeyStore ks, @NonNull String path, char[] pwd) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {

        if (ks.getType().equals("PKCS11")) {
            ks.store(null, pwd);
        } else {
            try (OutputStream out = Files.newOutputStream(Paths.get(path))) {
                ks.store(out, pwd);
            }
        }
    }

    /**
     * Transfers a certificate from one KeyStore to another.
     *
     * @param src       The source KeyStore.
     * @param srcAlias  The alias of the certificate in the source KeyStore.
     * @param dest      The destination KeyStore.
     * @param destAlias The alias for the certificate in the destination KeyStore.
     */
    public static void transferCert(@NonNull KeyStore src, @NonNull String srcAlias, @NonNull KeyStore dest, @NonNull String destAlias) throws KeyStoreException {
        Certificate crt;

        if (!src.containsAlias(srcAlias) || (crt = src.getCertificate(srcAlias)) == null)
            throw new RuntimeException("Alias " + srcAlias + " does not exist in src");

        dest.setCertificateEntry(destAlias, crt);
    }

    /**
     * Transfers a key from one KeyStore to another.
     *
     * @param src       The source KeyStore.
     * @param srcAlias  The alias of the key in the source KeyStore.
     * @param dest      The destination KeyStore.
     * @param destAlias The alias for the key in the destination KeyStore.
     * @param pwd       The password for the key in the source KeyStore.
     */
    public static void transferKey(@NonNull KeyStore src, @NonNull String srcAlias, @NonNull KeyStore dest, @NonNull String destAlias, char[] pwd) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        Key key;

        if ((key = src.getKey(srcAlias, pwd)) == null)
            throw new RuntimeException("Alias " + srcAlias + " does not exist in src");

        dest.setKeyEntry(destAlias, key, pwd, src.getCertificateChain(srcAlias));
    }

    /**
     * Retrieves a KeyPair from a KeyStore using the specified alias.
     *
     * @param alias    The alias of the KeyPair in the KeyStore.
     * @param keyStore The KeyStore from which to retrieve the KeyPair.
     * @return The KeyPair associated with the specified alias.
     * @throws KeyStoreException         If there is an issue with the KeyStore.
     * @throws UnrecoverableKeyException If the key is unrecoverable.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     */
    public static KeyPair getKeyPair(@NonNull String alias, @NonNull KeyStore keyStore) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        if (!keyStore.containsAlias(alias)) {
            throw new IllegalArgumentException("Alias " + alias + " does not exist in KeyStore");
        }

        return new KeyPair(keyStore.getCertificate(alias).getPublicKey(), (PrivateKey) keyStore.getKey(alias, "".toCharArray()));
    }

    /**
     * Infers/Guesses the KeyStore type from the file name extension.
     *
     * @param filename The file name or path.
     * @return The inferred KeyStore type.
     * @throws RuntimeException If the type cannot be inferred from the file name.
     */
    @NonNull
    public static String inferTypeFromFile(String filename) {
        if (filename == null) return "PKCS11";
        else if (filename.endsWith("jks") || filename.endsWith("keystore")) return "JKS";
        else if (filename.endsWith("p12") || filename.endsWith("pfx")) return "PKCS12";
        else
            throw new IllegalArgumentException("Cannot infer keystore type from file name, please used either .p12, .jks, or .keystore");
    }

}
