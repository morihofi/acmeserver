package de.morihofi.acmeserver.certificate.tools;

import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Crypto {

    public static String createNonce() {
        String nonce = "";

        try {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            String randomNum = String.valueOf(prng.nextInt());

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] result = sha.digest(randomNum.getBytes());
            nonce = hexEncode(result);
        } catch (Exception e) {
        }

        return nonce;
    }
    public static String hexEncode(byte[] aInput) {
        StringBuilder result = new StringBuilder();

        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };

        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }

        return result.toString();
    }

    public static KeyPair generateKeyPair(String algorithm, int keySize) throws Exception {
        java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance(algorithm, "BC");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    public static String encodeRSAPublicKey(RSAPublicKey key) throws Exception {
        byte[] encoded = key.getEncoded();
        String encodedString = Base64.toBase64String(encoded)
                .replace("+", "-").replace("/", "_").replace("=", "");
        return "-----BEGIN PUBLIC KEY-----\n" + chunkString(encodedString, 64) + "\n-----END PUBLIC KEY-----";
    }

    public static String encodeRSAPrivateKey(RSAPrivateKey key) throws Exception {
        byte[] encoded = key.getEncoded();
        String encodedString = Base64.toBase64String(encoded)
                .replace("+", "-").replace("/", "_")
                .replace("=", "");
        return "-----BEGIN PRIVATE KEY-----\n" + chunkString(encodedString, 64) + "\n-----END PRIVATE KEY-----";
    }
    private static String chunkString(String str, int chunkSize) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        while (index < str.length()) {
            builder.append(str.substring(index, Math.min(index + chunkSize, str.length()))).append("\n");
            index += chunkSize;
        }
        return builder.toString();
    }


    public static String hashStringSHA256(String stringToHash) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));


        return bytesToHex(encodedHash);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
