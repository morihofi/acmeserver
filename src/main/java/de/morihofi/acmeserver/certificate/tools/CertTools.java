package de.morihofi.acmeserver.certificate.tools;

import de.morihofi.acmeserver.certificate.JWSTestSignExample;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
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

    /**
     * Given a Keystore containing a private key and certificate and a Reader containing a PEM-encoded
     * Certificiate Signing Request (CSR), sign the CSR with that private key and return the signed
     * certificate as a PEM-encoded PKCS#7 signedData object.
     *
     * @param csrBase64                    Base64 encoded CSR
     * @param expireDate                   Expiration Date for the certificate
     * @param intermediateKeypair          KeyPair of the signing ca (can also be root ca, but not recommended)
     * @param intermediateCertificateBytes Certificate of the intermediate CA (or Root CA)
     * @return a String containing the PEM-encoded signed Certificate (begins "-----BEGIN PKCS #7 SIGNED DATA-----")
     */
    public static String signCSR(String csrBase64, Date expireDate, KeyPair intermediateKeypair, X509Certificate intermediateCertificateBytes) throws Exception {

        byte[] csrBytes = decodeBase64URLAsBytes(csrBase64);
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);
        X509Certificate caCert = intermediateCertificateBytes;

        X500Name issuer = X509.getX500NameFromX509Certificate(caCert);
        BigInteger serial = new BigInteger(32, new SecureRandom());


        /*

        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");

        Date from = new Date();




        X509v3CertificateBuilder certgen = new X509v3CertificateBuilder(issuer, serial, from, expireDate, csr.getSubject(), csr.getSubjectPublicKeyInfo());
        certgen.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        certgen.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(csr.getSubjectPublicKeyInfo().getEncoded()));
        certgen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        //       certgen.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(new GeneralNames(new GeneralName(new X509Name(cacert.getSubjectX500Principal().getName()))), cacert.getSerialNumber()));

        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(PrivateKeyFactory.createKey(intermediateKeypair.getPrivate().getEncoded()));
        X509CertificateHolder holder = certgen.build(signer);
        byte[] certencoded = holder.toASN1Structure().getEncoded();

        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        signer = new JcaContentSignerBuilder("SHA1withRSA").build(intermediateKeypair.getPrivate());
        generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(signer, caCert));
        generator.addCertificate(new X509CertificateHolder(certencoded));
        generator.addCertificate(new X509CertificateHolder(caCert.getEncoded()));
        CMSTypedData content = new CMSProcessableByteArray(certencoded);
        CMSSignedData signeddata = generator.generate(content, true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("-----BEGIN PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
        out.write(Base64Tools.encodeBase64(signeddata.getEncoded()).getBytes("ISO-8859-1"));
        out.write("\n-----END PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
        out.close();
        return new String(out.toByteArray(), "ISO-8859-1");

         */

/*
        SubjectPublicKeyInfo spi = csr.getSubjectPublicKeyInfo();

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuer,
                serial,
                new java.util.Date(),
                expireDate,
                csr.getSubject(),
                csr.getSubjectPublicKeyInfo());

        certBuilder.addExtension(Extension.subjectKeyIdentifier,false, spi);

        PrivateKey issuerPK = intermediateKeypair.getPrivate();
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(issuerPK);
        X509Certificate x509 = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certBuilder.build(contentSigner));

        return certificateToPEM(x509.getEncoded());
*/


        return "CURRENTLY NOT IMPLEMENTED";
    }

    public static byte[] generateCertificateAuthorityCertificate(String commonName, int years, KeyPair kp) throws IOException, NoSuchAlgorithmException, OperatorCreationException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, years);

        byte[] pk = kp.getPublic().getEncoded();
        SubjectPublicKeyInfo bcPk = SubjectPublicKeyInfo.getInstance(pk);

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(new X500Name("CN=" + commonName), BigInteger.ONE, new Date(), cal.getTime(), new X500Name("CN=" + commonName), bcPk);

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        certBuilder.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign | KeyUsage.cRLSign));


        X509CertificateHolder certHolder = certBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate()));


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
        //certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
        certBuilder.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyCertSign | KeyUsage.cRLSign));
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

}
