package de.morihofi.acmeserver.tools;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
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
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.security.cert.CertificateEncodingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

public class CertTools {


    public static KeyPair generateRSAKeyPair(int rsaKeySize) throws NoSuchAlgorithmException {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(rsaKeySize);
        KeyPair kp = rsa.generateKeyPair();

        return kp;
    }


    public static X509Certificate convertToX509Cert(String certificateString) throws CertificateException {
        X509Certificate certificate = null;
        CertificateFactory cf = null;
        try {
            if (certificateString != null && !certificateString.trim().isEmpty()) {

                System.out.println("Before:");
                System.out.println(certificateString);

                certificateString = certificateString.replace("-----BEGIN CERTIFICATE-----\n", "").replace("-----END CERTIFICATE-----", ""); // NEED FOR PEM FORMAT CERT STRING

                System.out.println("Trimmed:");
                System.out.println(certificateString);


                byte[] certificateData = Base64.getDecoder().decode(certificateString);
                cf = CertificateFactory.getInstance("X509");
                certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
            }
        } catch (CertificateException e) {
            throw new CertificateException(e);
        }
        return certificate;
    }

    public static X509Certificate convertToX509Cert(byte[] certificateBytes) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateBytes));
        return certificate;
    }


    public static byte[] generateCertificateAuthorityCertificate(String commonName, int years, KeyPair kp) throws IOException, NoSuchAlgorithmException, OperatorCreationException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, years);

        byte[] pk = kp.getPublic().getEncoded();
        SubjectPublicKeyInfo bcPk = SubjectPublicKeyInfo.getInstance(pk);

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(new X500Name("CN=" + commonName), BigInteger.valueOf((long) (System.currentTimeMillis() * 1.1)), new Date(), cal.getTime(), new X500Name("CN=" + commonName), bcPk);

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        certBuilder.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign | KeyUsage.cRLSign));


        X509CertificateHolder certHolder = certBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate()));


        return certHolder.getEncoded();

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

    public static X509Certificate createIntermediateCertificate(KeyPair caKeyPair, byte[] caCertificateBytes, KeyPair intermediateKeyPair, String intermediateCommonName, int days, int months, int years) throws CertIOException, CertificateException, OperatorCreationException {

        // Create like a CSR
        X500Name issuerName = X509.getX500NameFromX509Certificate(convertToX509Cert(caCertificateBytes)); // new X500Name("CN=YourCA");
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date startDate = new Date(); // Starts now

        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        // manipulate date
        c.add(Calendar.YEAR, years);
        c.add(Calendar.MONTH, months);
        c.add(Calendar.DATE, days);

        // Set new expire date
        Date endDate = c.getTime();

        X500Name subjectName = new X500Name("CN=" + intermediateCommonName);
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuerName, serialNumber, startDate, endDate, subjectName, SubjectPublicKeyInfo.getInstance(intermediateKeyPair.getPublic().getEncoded()));

        // BasicConstraints Extension, um das Intermediate Certificate als CA zu kennzeichnen
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        // KeyUsage Extension, um das Intermediate Certificate als Zertifikatssignatur zu kennzeichnen
        //certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign | KeyUsage.cRLSign));
        // ******************************************

        // Sign this "CSR" with our CA
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(caKeyPair.getPrivate());

        X509CertificateHolder holder = certBuilder.build(signer);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate intermediateCert = converter.getCertificate(holder);

        return intermediateCert;
    }

    /**
     * Creates a SSL/TLS Certificate for a webserver
     *
     * @param intermediateKeyPair          KeyPair from Intermediate Certificate
     * @param intermediateCertificateBytes Certificate byte array of the intermediate certificate
     * @param serverPublicKeyBytes         Public Key byte array, that the web server uses
     * @param dnsNames                     Array of DNS Names for the Certificate
     * @param days                         How many days in the future is it valid
     * @param months                       How many months in the future is it valid
     * @param years                        How many years in the future is it valid
     * @return Certificate for the Server
     * @throws CertificateException
     * @throws CertIOException
     * @throws OperatorCreationException
     */
    public static X509Certificate createServerCertificate(KeyPair intermediateKeyPair, byte[] intermediateCertificateBytes, byte[] serverPublicKeyBytes, String dnsNames[], int days, int months, int years) throws CertificateException, CertIOException, OperatorCreationException {

        // Create our virtual "CSR"

        X500Name issuerName = X509.getX500NameFromX509Certificate(convertToX509Cert(intermediateCertificateBytes));
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        Date startDate = new Date(); // Starts now

        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        // manipulate date
        c.add(Calendar.YEAR, years);
        c.add(Calendar.MONTH, months);
        c.add(Calendar.DATE, days);

        // Set new expire date
        Date endDate = c.getTime();

        X500Name subjectName = new X500Name("CN=" + dnsNames[0]);
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuerName, serialNumber, startDate, endDate, subjectName, SubjectPublicKeyInfo.getInstance(serverPublicKeyBytes));

        // Client Certificate -> This is no CA
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // Add a entry DNS Name requested
        ArrayList<GeneralName> dnsGeneralNameList = new ArrayList<>();

        for (String dnsName : dnsNames) {
            dnsGeneralNameList.add(new GeneralName(GeneralName.dNSName, dnsName));
        }
        GeneralNames subjectAltNames = new GeneralNames(dnsGeneralNameList.toArray(new GeneralName[0]));

        //GeneralNames subjectAltNames = new GeneralNames(new GeneralName(GeneralName.dNSName, dnsNames[0]));
        certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        // ******************************************

        // Sign this "CSR" with our CA
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(intermediateKeyPair.getPrivate());

        X509CertificateHolder holder = certBuilder.build(signer);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate serverCert = converter.getCertificate(holder);

        return serverCert;
    }


    public static String decodeBase64URL(String stringToDecode) {

        // Decoding URl
        String dStr = new String(decodeBase64URLAsBytes(stringToDecode));
        return dStr;

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
        String eStr = encoder.encodeToString(stringToEncode.getBytes());

        System.out.println("Encoded URL: " + eStr);

        return eStr;
    }


    /**
     * Liest ein Zertifikat aus einer PEM-Datei und gibt die Zertifikatsbytes zurück.
     * @param certificatePath Der Pfad zur PEM-Datei.
     * @param keyPair Das KeyPair, für das das Zertifikat ausgelesen werden soll.
     * @return Ein Byte-Array, das das Zertifikat darstellt.
     * @throws IOException Wenn ein I/O-Fehler auftritt.
     * @throws CertificateEncodingException Wenn ein Fehler beim Encodieren des Zertifikats auftritt.
     */
    public static byte[] getCertificateBytes(Path certificatePath, KeyPair keyPair) throws IOException, CertificateEncodingException, CertificateException {
        // Lesen Sie die Datei mit dem PEMParser
        try (PEMParser pemParser = new PEMParser(Files.newBufferedReader(certificatePath))) {
            Object pemObject = pemParser.readObject();
            if (pemObject instanceof X509CertificateHolder) {
                // Erstellen Sie eine CertificateFactory für X.509
                CertificateFactory factory = CertificateFactory.getInstance("X.509");

                // Konvertieren Sie das gelesene Objekt in ein X509Certificate
                X509CertificateHolder certificateHolder = (X509CertificateHolder) pemObject;
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
        PemWriter pemWriter = new PemWriter(stringWriter);

        try {
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        } finally {
            pemWriter.close();
        }

        return stringWriter.toString();
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

}