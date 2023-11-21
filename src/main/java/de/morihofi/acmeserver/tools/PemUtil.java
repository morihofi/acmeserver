package de.morihofi.acmeserver.tools;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

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
    public static void saveKeyPairToPEM(KeyPair keyPair, Path publicKeyFilePath, Path privateKeyFilePath) throws IOException {
        try (JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(Files.newOutputStream(privateKeyFilePath), StandardCharsets.UTF_8))) {
            writer.writeObject(keyPair.getPrivate());
        }

        try (JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(Files.newOutputStream(publicKeyFilePath), StandardCharsets.UTF_8))) {
            writer.writeObject(keyPair.getPublic());
        }
    }

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

    private static PublicKey readPublicKeyFromFile(Path file) throws IOException {
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            Object object = pemParser.readObject();
            return converter.getPublicKey((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) object);
        }
    }

    public static KeyPair loadKeyPair(Path privateKeyFile, Path publicKeyFile) throws IOException {
        PrivateKey privateKey = readPrivateKeyFromFile(privateKeyFile);
        PublicKey publicKey = readPublicKeyFromFile(publicKeyFile);
        return new KeyPair(publicKey, privateKey);
    }

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


}
