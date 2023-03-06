package de.morihofi.acmeserver.certificate;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

        X509v1CertificateBuilder certGen = new X509v1CertificateBuilder(new X500Name("CN=" + commonName), BigInteger.ONE, new Date(), cal.getTime(), new X500Name("CN=" + commonName), bcPk);

        X509CertificateHolder certHolder = certGen.build(new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate()));

        return certHolder.getEncoded();

    }

    public static String certificateToPEM(byte[] certificate) {
        BASE64Encoder encoder = new BASE64Encoder();

        StringBuilder builder = new StringBuilder();

        builder.append(X509Factory.BEGIN_CERT + "\n");
        builder.append(encoder.encodeBuffer(certificate).replace("\r\n", "\n"));
        builder.append(X509Factory.END_CERT + "\n");

        return builder.toString();
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
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));


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
     * @param intermediateKeyPair KeyPair from Intermediate Certificate
     * @param intermediateCertificateBytes Certificate byte array of the intermediate certificate
     * @param serverPublicKeyBytes Public Key byte array, that the web server uses
     * @param dnsNames
     * @param days How many days in the future is it valid
     * @param months How many months in the future is it valid
     * @param years How many years in the future is it valid
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
        certBuilder.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // Add a entry DNS Name requested
        ArrayList<GeneralName> dnsGeneralNameList = new ArrayList<>();

        for (String dnsName: dnsNames) {
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
}
