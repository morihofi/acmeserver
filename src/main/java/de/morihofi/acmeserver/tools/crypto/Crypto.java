/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.crypto;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class Crypto {

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
     * This method uses a {@link SecureRandom} instance to generate a random 130-bit value, which is then converted into a hexadecimal
     * string.
     *
     * @return A random, unique identifier as a String.
     */
    public static String generateRandomId() {
        SecureRandom secureRandom = new SecureRandom();
        return new BigInteger(130, secureRandom).toString(32); // 32 for hexadecimal representation
    }

    private Crypto() {}
}
