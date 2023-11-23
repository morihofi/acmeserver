package de.morihofi.acmeserver.tools.certificate;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.config.CertificateConfig;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class CertTools {


    public static KeyPair generateRSAKeyPair(int rsaKeySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        rsa.initialize(rsaKeySize);

        return rsa.generateKeyPair();
    }

    public static KeyPair generateEcdsaKeyPair(String curveName) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);

        // Der Parameter "secp256k1" kann je nach gewünschter EC-Kurve geändert werden
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(curveName);
        keyPairGenerator.initialize(ecSpec);

        return keyPairGenerator.generateKeyPair();
    }


    public static X509Certificate convertToX509Cert(byte[] certificateBytes) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateBytes));
        return certificate;
    }


    public static String certificatesChainToPEM(byte[][] certificateBytesArray) {
        try {
            // Erstellen eines StringWriter, um die gesamte Zertifikatskette zu speichern
            StringWriter stringWriter = new StringWriter();

            // Durchlaufen jedes Zertifikats in der Kette
            for (byte[] certificateBytes : certificateBytesArray) {
                // Erstellen eines PemObject für jedes Zertifikat
                stringWriter.write(certificateToPEM(certificateBytes));
                // Füge eine Leerzeile zwischen den Zertifikaten hinzu für bessere Lesbarkeit
                stringWriter.write("\n");
            }

            // Rückgabe der gesamten PEM-Repräsentation der Zertifikatskette
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Konvertieren der Zertifikatskette in PEM-Format", e);
        }
    }


    public static String certificateToPEM(byte[] certificateBytes) {
        try {
            // Erstellen eines PemObject mit dem Zertifikat
            PemObject pemObject = new PemObject("CERTIFICATE", certificateBytes);

            // Erstellen eines StringWriter und eines PemWriter
            StringWriter stringWriter = new StringWriter();
            try (PemWriter pemWriter = new PemWriter(stringWriter)) {
                // Schreiben des PemObject in den PemWriter
                pemWriter.writeObject(pemObject);
            }

            // Rückgabe der PEM-Repräsentation des Zertifikats
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Konvertieren des Zertifikats in PEM-Format", e);
        }
    }


    public static String decodeBase64URL(String stringToDecode) {

        // Decoding URl
        return new String(decodeBase64URLAsBytes(stringToDecode), StandardCharsets.UTF_8);

    }

    public static byte[] decodeBase64URLAsBytes(String stringToDecode) {
        // Getting decoder
        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
        return decoder.decode(stringToDecode);

    }


    public static String encodedBase64URL(String stringToEncode) {

        // Getting encoder
        java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder();

        // Encoding URL
        String eStr = encoder.encodeToString(stringToEncode.getBytes(StandardCharsets.UTF_8));

        System.out.println("Encoded URL: " + eStr);

        return eStr;
    }


    /**
     * Liest ein Zertifikat aus einer PEM-Datei und gibt die Zertifikatsbytes zurück.
     *
     * @param certificatePath Der Pfad zur PEM-Datei.
     * @param keyPair         Das KeyPair, für das das Zertifikat ausgelesen werden soll.
     * @return Ein Byte-Array, das das Zertifikat darstellt.
     * @throws IOException Wenn ein I/O-Fehler auftritt.
     */
    public static byte[] getCertificateBytes(Path certificatePath, KeyPair keyPair) throws IOException, CertificateException {
        // Lesen Sie die Datei mit dem PEMParser
        try (PEMParser pemParser = new PEMParser(Files.newBufferedReader(certificatePath))) {
            Object pemObject = pemParser.readObject();
            if (pemObject instanceof X509CertificateHolder certificateHolder) {
                // Konvertieren Sie das gelesene Objekt in ein X509Certificate
                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);

                // Prüfen Sie, ob das Zertifikat zum öffentlichen Schlüssel passt
                if (!certificate.getPublicKey().equals(keyPair.getPublic())) {
                    throw new IllegalArgumentException("The public certificate does not match the specified public key.");
                }

                // Geben Sie die Zertifikatsbytes zurück
                return certificate.getEncoded();
            } else {
                throw new IllegalArgumentException("The specified file does not contain a valid certificate.");
            }
        }
    }


    public static String convertToPem(PublicKey publicKey) throws IOException {
        StringWriter stringWriter = new StringWriter();

        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        }

        return stringWriter.toString();
    }

    public static byte[] convertPemToByteArray(String pemString) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader pemReader = new PemReader(new StringReader(pemString));
        PemObject pemObject = pemReader.readPemObject();
        byte[] content = pemObject.getContent();
        pemReader.close();

        // Optional: Convert to PublicKey if needed
        // PublicKey publicKey = convertToPublicKey(content);

        return content;
    }

    // Optional: Convert byte array to PublicKey
    public static PublicKey convertToPublicKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static PublicKey convertToPublicKey(String pemString) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader pemReader = new PemReader(new StringReader(pemString));
        byte[] content = pemReader.readPemObject().getContent();
        pemReader.close();

        // Umwandeln in ein SubjectPublicKeyInfo Objekt
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(content);

        // Ermitteln des Algorithmus OIDs
        AlgorithmIdentifier algorithmIdentifier = subjectPublicKeyInfo.getAlgorithm();
        ASN1ObjectIdentifier algorithmOid = algorithmIdentifier.getAlgorithm();

        // Entsprechenden KeyFactory erzeugen
        String keyAlgorithm;
        if (algorithmOid.on(new ASN1ObjectIdentifier("1.2.840.10045"))) { // OID für elliptische Kurven
            keyAlgorithm = "EC";
        } else if (algorithmOid.on(new ASN1ObjectIdentifier("1.2.840.113549.1.1"))) { // OID für RSA
            keyAlgorithm = "RSA";
        } else {
            throw new IllegalArgumentException("Unsupported Key Type");
        }

        // Erzeugen des PublicKey
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded());
        return keyFactory.generatePublic(spec);
    }


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


    public static PKCS10CertificationRequest createCSR(X509Certificate oldCertificate, PublicKey publicKey, PrivateKey privateKey) throws IOException, OperatorCreationException {
        X500Name subject = new X500Name(oldCertificate.getSubjectX500Principal().getName());
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());

        PKCS10CertificationRequestBuilder csrBuilder = new PKCS10CertificationRequestBuilder(subject, publicKeyInfo);

        // ExtensionsGenerator zum Hinzufügen der Erweiterungen zum CSR
        ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();

        // Erweiterungen aus dem alten Zertifikat hinzufügen
        addExtensionsFromCertificate(oldCertificate, extensionsGenerator);

        // CSR mit den Erweiterungen erstellen
        csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensionsGenerator.generate());

        // ContentSigner für das Signieren des CSRs erstellen
        String signatureAlgorithm;
        if (privateKey instanceof ECKey) {
            signatureAlgorithm = "SHA256withECDSA";
        } else if (privateKey instanceof RSAKey) {
            signatureAlgorithm = "SHA256withRSA";
        } else {
            throw new IllegalArgumentException("Unsupported key type");
        }

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);

        return csrBuilder.build(signer);
    }

    private static void addExtensionsFromCertificate(X509Certificate certificate, ExtensionsGenerator extensionsGenerator) throws IOException {
        Set<String> criticalOids = certificate.getCriticalExtensionOIDs();
        Set<String> nonCriticalOids = certificate.getNonCriticalExtensionOIDs();

        if (criticalOids != null) {
            for (String oid : criticalOids) {
                addExtension(certificate, oid, true, extensionsGenerator);
            }
        }

        if (nonCriticalOids != null) {
            for (String oid : nonCriticalOids) {
                addExtension(certificate, oid, false, extensionsGenerator);
            }
        }
    }

    private static void addExtension(X509Certificate certificate, String oidString, boolean isCritical, ExtensionsGenerator extensionsGenerator) throws IOException {
        ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(oidString);
        byte[] bytes = certificate.getExtensionValue(oidString);
        if (bytes != null) {
            ASN1Encodable value = ASN1ObjectIdentifier.fromByteArray(bytes);
            extensionsGenerator.addExtension(oid, isCritical, value);
        }
    }
}

