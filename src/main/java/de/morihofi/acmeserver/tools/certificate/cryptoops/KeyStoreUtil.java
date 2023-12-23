package de.morihofi.acmeserver.tools.certificate.cryptoops;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * A class handling KeyStore operations.
 * Original source <a href="https://github.com/gerritjvv/engsectools/blob/master/pkcs11/src/main/java/engsec/KeyStoreUtil.java">GitHub gerritjvv/engsectools</a> and modified some methods.
 */
public class KeyStoreUtil {

    /**
     * Saves the KeyStore to the specified path.
     *
     * @param ks   The KeyStore to be saved.
     * @param path The path where the KeyStore will be saved.
     * @param pwd  The password for the KeyStore.
     */
    public static void saveKeyStore(KeyStore ks, String path, char[] pwd) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {

            if (ks.getType().equals("PKCS11")) {
                ks.store(null, pwd);
            } else {
                try (OutputStream out = Files.newOutputStream(Paths.get(path))) {
                    ks.store(out, pwd);
                }
            }
    }

    /**
     * Loads a KeyStore inferring the type from the file name extension.
     *
     * @param path The path to the KeyStore file. If null, PKCS11 type is used.
     * @param pwd  The password for the KeyStore.
     * @return The loaded KeyStore.
     */
    public static KeyStore loadKeyStore(String path, char[] pwd) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

            KeyStore keyStore = KeyStore.getInstance(path == null ? "PKCS11" : inferTypeFromFile(path));

            if (path == null) {
                keyStore.load(null, pwd);
            } else {
                try (InputStream in = Files.newInputStream(Paths.get(path))) {
                    keyStore.load(in, pwd);
                }
            }

            return keyStore;

    }

    /**
     * Transfers a certificate from one KeyStore to another.
     *
     * @param src       The source KeyStore.
     * @param srcAlias  The alias of the certificate in the source KeyStore.
     * @param dest      The destination KeyStore.
     * @param destAlias The alias for the certificate in the destination KeyStore.
     */
    public static void transferCert(KeyStore src, String srcAlias, KeyStore dest, String destAlias) throws KeyStoreException {
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
    public static void transferKey(KeyStore src, String srcAlias, KeyStore dest, String destAlias, char[] pwd) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
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
    public static KeyPair getKeyPair(String alias, KeyStore keyStore) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        if (!keyStore.containsAlias(alias)) {
            throw new IllegalArgumentException("Alias " + alias + " does not exist in KeyStore");
        }

        return new KeyPair(
                keyStore.getCertificate(alias).getPublicKey(),
                (PrivateKey) keyStore.getKey(alias, "".toCharArray())
        );
    }

    /**
     * Infers the KeyStore type from the file name extension.
     *
     * @param file The file name or path.
     * @return The inferred KeyStore type.
     * @throws RuntimeException If the type cannot be inferred from the file name.
     */
    public static String inferTypeFromFile(String file) {
        if (file == null) return "PKCS11";
        else if (file.endsWith("jks") || file.endsWith("keystore")) return "JKS";
        else if (file.endsWith("p12") || file.endsWith("pfx")) return "PKCS12";
        else
            throw new IllegalArgumentException("Cannot infer keystore type from file name, please used either .p12, .jks, or .keystore");
    }


    /**
     * A functional interface for a supplier that may throw a checked exception.
     *
     * @param <T> The type of the result.
     * @param <E> The type of the checked exception that may be thrown.
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    /**
     * A functional interface for a runnable that may throw a checked exception.
     *
     * @param <E> The type of the checked exception that may be thrown.
     */
    @FunctionalInterface
    public interface ThrowingRunnable<E extends Throwable> {
        void run() throws E;
    }
}