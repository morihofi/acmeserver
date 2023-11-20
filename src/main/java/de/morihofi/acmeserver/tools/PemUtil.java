package de.morihofi.acmeserver.tools;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.*;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class PemUtil {
    public static void saveKeyPairToPEM(KeyPair keyPair, Path publicKeyFilePath, Path privateKeyFilePath) throws IOException {
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(privateKeyFilePath.toFile()))) {
            writer.writeObject(keyPair.getPrivate());
        }

        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(publicKeyFilePath.toFile()))) {
            writer.writeObject(keyPair.getPublic());
        }
    }

    private static PrivateKey readPrivateKeyFromFile(Path file) throws IOException {
        try (PEMParser pemParser = new PEMParser(new FileReader(file.toFile()))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            Object object = pemParser.readObject();

            // Überprüfen, ob das Objekt ein PrivateKeyInfo oder ein PemKeyPair ist
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
        try (PEMParser pemParser = new PEMParser(new FileReader(file.toFile()))) {
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

}
