package de.morihofi.acmeserver.tools.certificate;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class PemUtil {

    /**
     * Saves a KeyPair to PEM-encoded files.
     *
     * @param keyPair            The KeyPair to be saved.
     * @param publicKeyFilePath  The path to save the public key in PEM format.
     * @param privateKeyFilePath The path to save the private key in PEM format.
     * @throws IOException If there is an issue writing the keys to the files.
     */
    public static void saveKeyPairToPEM(KeyPair keyPair, Path publicKeyFilePath, Path privateKeyFilePath) throws IOException {
        try (JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(Files.newOutputStream(privateKeyFilePath), StandardCharsets.UTF_8))) {
            writer.writeObject(keyPair.getPrivate());
        }

        try (JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(Files.newOutputStream(publicKeyFilePath), StandardCharsets.UTF_8))) {
            writer.writeObject(keyPair.getPublic());
        }
    }

    /**
     * Reads a private key from a PEM-encoded file.
     *
     * @param file The path to the PEM-encoded file containing the private key.
     * @return The PrivateKey read from the file.
     * @throws IOException              If there is an issue reading the file or parsing the key.
     * @throws IllegalArgumentException If the file does not contain a valid private key.
     */
    private static PrivateKey readPrivateKeyFromFile(Path file) throws IOException {
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            Object object = pemParser.readObject();

            if (object instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) object);
            } else if (object instanceof PEMKeyPair) {
                return converter.getPrivateKey(((PEMKeyPair) object).getPrivateKeyInfo());
            } else {
                throw new IllegalArgumentException("File does not contain a valid private key");
            }
        }
    }

    /**
     * Reads a public key from a PEM-encoded file.
     *
     * @param file The path to the PEM-encoded file containing the public key.
     * @return The PublicKey read from the file.
     * @throws IOException If there is an issue reading the file or parsing the key.
     */
    private static PublicKey readPublicKeyFromFile(Path file) throws IOException {
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            Object object = pemParser.readObject();
            return converter.getPublicKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) object);
        }
    }

    /**
     * Loads a KeyPair from separate files containing the private and public keys.
     *
     * @param privateKeyFile The path to the file containing the private key.
     * @param publicKeyFile  The path to the file containing the public key.
     * @return A KeyPair containing the loaded public and private keys.
     * @throws IOException If there is an issue reading the key files.
     */
    public static KeyPair loadKeyPair(Path privateKeyFile, Path publicKeyFile) throws IOException {
        PrivateKey privateKey = readPrivateKeyFromFile(privateKeyFile);
        PublicKey publicKey = readPublicKeyFromFile(publicKeyFile);
        return new KeyPair(publicKey, privateKey);
    }


    /**
     * Reads a public key from a PEM-encoded string and returns it as a PublicKey object.
     *
     * @param pemKey The PEM-encoded public key string.
     * @return A PublicKey object representing the parsed public key.
     * @throws IOException              If there is an issue reading the PEM-encoded key.
     * @throws NoSuchAlgorithmException If the key algorithm is not supported.
     * @throws NoSuchProviderException  If there is no security provider available for the key algorithm.
     * @throws InvalidKeySpecException  If there is an issue parsing the key specification.
     */
    public static PublicKey readPublicKeyFromPem(String pemKey) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        PEMParser pemParser = new PEMParser(new StringReader(pemKey));
        Object object = pemParser.readObject();
        pemParser.close();

        SubjectPublicKeyInfo publicKeyInfo = (SubjectPublicKeyInfo) object;
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyInfo.getEncoded());

        String algorithm = publicKeyInfo.getAlgorithm().getAlgorithm().getId();
        KeyFactory keyFactory;

        if (algorithm.equals("1.2.840.10045.2.1")) { // ECDSA
            keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        } else if (algorithm.equals("1.2.840.113549.1.1.1")) { // RSA
            keyFactory = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        } else {
            throw new NoSuchAlgorithmException("Unsupported algorithm: " + algorithm);
        }

        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * Loads a chain of X.509 certificates from a PEM file and returns them as an array of X.509 certificate objects.
     *
     * @param pemFilePath The path to the PEM file containing the X.509 certificate chain.
     * @return An array of X509Certificate objects representing the parsed X.509 certificate chain.
     * @throws IOException             If there is an issue reading the PEM file.
     * @throws CertificateException    If there is an issue parsing the X.509 certificates from the PEM file.
     * @throws NoSuchProviderException If there is no security provider available for X.509 certificates.
     */
    public static X509Certificate[] loadCertificateChain(Path pemFilePath) throws IOException, CertificateException, NoSuchProviderException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);

        try (PEMParser pemParser = new PEMParser(Files.newBufferedReader(pemFilePath))) {
            List<X509Certificate> certificates = new ArrayList<>();
            Object object;

            while ((object = pemParser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
                    X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
                            new ByteArrayInputStream(certificateHolder.getEncoded())
                    );
                    certificates.add(certificate);
                }
            }

            return certificates.toArray(new X509Certificate[0]);
        }
    }


    /**
     * Converts an array of byte arrays containing certificates into a PEM-encoded string representing a certificate chain.
     *
     * @param certificateBytesArray An array of byte arrays where each element represents a certificate in the chain.
     * @return The PEM-encoded representation of the certificate chain.
     * @throws RuntimeException If there is an issue converting the certificate chain.
     */
    public static String certificatesChainToPEM(byte[][] certificateBytesArray) {
        try {
            // Create a StringWriter to store the entire certificate chain
            StringWriter stringWriter = new StringWriter();

            // Iterate through each certificate in the chain
            for (byte[] certificateBytes : certificateBytesArray) {
                // Create a PemObject for each certificate
                stringWriter.write(certificateToPEM(certificateBytes));
                // Add a newline between certificates for better readability
                stringWriter.write("\n");
            }

            // Return the entire PEM-encoded representation of the certificate chain
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error converting the certificate chain to PEM format", e);
        }
    }


    /**
     * Converts a byte array containing a certificate into a PEM-encoded string.
     *
     * @param certificateBytes The byte array representing the certificate.
     * @return The PEM-encoded representation of the certificate.
     * @throws IOException If there is an issue converting the certificate.
     */
    public static String certificateToPEM(byte[] certificateBytes) throws IOException {
        // Create a PemObject with the certificate bytes
        PemObject pemObject = new PemObject("CERTIFICATE", certificateBytes);

        // Create a StringWriter and a PemWriter
        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            // Write the PemObject to the PemWriter
            pemWriter.writeObject(pemObject);
        }

        // Return the PEM-encoded representation of the certificate
        return stringWriter.toString();
    }

    /**
     * Converts a public key to its PEM format representation.
     *
     * @param publicKey The public key to be converted to PEM format.
     * @return The PEM format representation of the public key.
     * @throws IOException If there is an issue during the conversion process.
     */
    public static String convertToPem(PublicKey publicKey) throws IOException {
        StringWriter stringWriter = new StringWriter();

        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        }

        return stringWriter.toString();
    }


    /**
     * Converts a PEM-encoded string to a byte array containing the binary content.
     *
     * @param pemString The PEM-encoded string to be converted.
     * @return A byte array containing the binary content from the PEM-encoded string.
     * @throws IOException If there is an issue during the conversion process.
     */
    public static byte[] convertPemToByteArray(String pemString) throws IOException {
        PemReader pemReader = new PemReader(new StringReader(pemString));
        PemObject pemObject = pemReader.readPemObject();
        byte[] content = pemObject.getContent();
        pemReader.close();

        return content;
    }

}
