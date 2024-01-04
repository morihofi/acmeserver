package de.morihofi.acmeserver.tools.certificate;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class CertTools {

    private CertTools(){}

    /**
     * Reads a certificate file in PEM format, converts it to X.509 format, and returns the certificate bytes.
     *
     * @param certificatePath The path to the certificate file in PEM format.
     * @param keyPair         The key pair containing the public key that should match the certificate's public key.
     * @return The bytes of the X.509 certificate.
     * @throws IOException              If there is an issue reading the certificate file.
     * @throws CertificateException     If there is an issue with the certificate format or content.
     * @throws IllegalArgumentException If the certificate's public key does not match the specified public key.
     */
    public static byte[] getCertificateBytes(Path certificatePath, KeyPair keyPair) throws IOException, CertificateException {
        try (PEMParser pemParser = new PEMParser(Files.newBufferedReader(certificatePath))) {
            Object pemObject = pemParser.readObject();
            if (pemObject instanceof X509CertificateHolder certificateHolder) {
                // Convert the read object to an X509Certificate
                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);

                // Check if the certificate's public key matches the specified public key
                if (!certificate.getPublicKey().equals(keyPair.getPublic())) {
                    throw new IllegalArgumentException("The public certificate does not match the specified public key.");
                }

                // Return the certificate bytes in X.509 format
                return certificate.getEncoded();
            } else {
                throw new IllegalArgumentException("The specified file does not contain a valid certificate.");
            }
        }
    }

    /**
     * Converts a PEM-encoded public key string to a PublicKey object.
     *
     * @param pemString The PEM-encoded public key string to be converted.
     * @return A PublicKey object representing the converted public key.
     * @throws IOException              If there is an issue during the conversion process.
     * @throws NoSuchAlgorithmException If the cryptographic algorithm is not available.
     * @throws InvalidKeySpecException  If the provided key specification is invalid.
     * @throws IllegalArgumentException If the key type is unsupported.
     */
    public static PublicKey convertToPublicKey(String pemString) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader pemReader = new PemReader(new StringReader(pemString));
        byte[] content = pemReader.readPemObject().getContent();
        pemReader.close();

        // Convert to a SubjectPublicKeyInfo object
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(content);

        // Determine the algorithm OIDs
        AlgorithmIdentifier algorithmIdentifier = subjectPublicKeyInfo.getAlgorithm();
        ASN1ObjectIdentifier algorithmOid = algorithmIdentifier.getAlgorithm();

        // Determine the key algorithm
        String keyAlgorithm;
        if (algorithmOid.on(new ASN1ObjectIdentifier("1.2.840.10045"))) { // OID for elliptic curves
            keyAlgorithm = "EC";
        } else if (algorithmOid.on(new ASN1ObjectIdentifier("1.2.840.113549.1.1"))) { // OID for RSA
            keyAlgorithm = "RSA";
        } else {
            throw new IllegalArgumentException("Unsupported Key Type");
        }

        // Generate the PublicKey
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded());
        return keyFactory.generatePublic(spec);
    }


    /**
     * Converts a raw ECDSA signature to a DER-encoded signature.
     *
     * @param rawSignature The raw ECDSA signature to be converted.
     * @return A byte array containing the DER-encoded signature.
     * @throws IOException              If there is an issue during the conversion process.
     * @throws IllegalArgumentException If the input raw signature has an invalid length.
     */
    public static byte[] convertRawToDerSignatureECDSA(byte[] rawSignature) throws IOException {
        if (rawSignature.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid raw signature length");
        }

        int sLength = rawSignature.length / 2;
        byte[] rBytes = new byte[sLength];
        byte[] sBytes = new byte[sLength];

        System.arraycopy(rawSignature, 0, rBytes, 0, sLength);
        System.arraycopy(rawSignature, sLength, sBytes, 0, sLength);

        BigInteger r = new BigInteger(1, rBytes);
        BigInteger s = new BigInteger(1, sBytes);

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(r));
        v.add(new ASN1Integer(s));

        DERSequence derSignature = new DERSequence(v);
        return derSignature.getEncoded("DER");
    }
}

