package de.morihofi.acmeserver.tools.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Crypto {

    /**
     * Generates a nonce (number used once) for security purposes.
     *
     * @return A randomly generated nonce as a hexadecimal string.
     * @throws IllegalArgumentException If there is an issue creating the nonce.
     */
    public static String createNonce() {
        String nonce = "";

        try {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            String randomNum = String.valueOf(prng.nextInt());

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] result = sha.digest(randomNum.getBytes(StandardCharsets.UTF_8));
            nonce = hexEncode(result);

            return nonce;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create nonce", e);
        }

    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array to convert.
     * @return The hexadecimal representation of the byte array.
     */
    private static String hexEncode(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b & 0xff));
        }
        return hexString.toString();
    }


    /**
     * Calculates the SHA-256 hash of a given string.
     *
     * @param stringToHash The input string to calculate the hash for.
     * @return A hexadecimal representation of the SHA-256 hash of the input string.
     * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available on the system.
     */
    public static String hashStringSHA256(String stringToHash) throws NoSuchAlgorithmException {
        // Initialize a MessageDigest with the SHA-256 algorithm
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Compute the hash of the input string as bytes
        byte[] encodedHash = digest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));

        // Convert the byte array to a hexadecimal string representation
        return hexEncode(encodedHash);
    }

}
