package de.morihofi.acmeserver.tools;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

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

}
