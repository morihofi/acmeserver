package de.morihofi.acmeserver.tools.crypto;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class Crypto {


    private Crypto(){}
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    /**
     * Generates a nonce (number used once) for security purposes.
     *
     * @return A randomly generated nonce as a hexadecimal string.
     * @throws IllegalArgumentException If there is an issue creating the nonce.
     */
    public static String createNonce() {
        try {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            String randomNum = String.valueOf(prng.nextInt());

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] result = sha.digest(randomNum.getBytes(StandardCharsets.UTF_8));

            return Hashing.hexEncode(result);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create nonce", e);
        }

    }

    /**
     * Generates a cryptographically strong random identifier.
     * <p>
     * This method uses a {@link SecureRandom} instance to generate a random 130-bit value,
     * which is then converted into a hexadecimal string.
     *
     * @return A random, unique identifier as a String.
     */

    public static String generateRandomId() {
        SecureRandom secureRandom = new SecureRandom();
        return new BigInteger(130, secureRandom).toString(32); // 32 for hexadecimal representation
    }


}
