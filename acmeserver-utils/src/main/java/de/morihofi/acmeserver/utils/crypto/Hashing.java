/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.utils.crypto;


import de.morihofi.acmeserver.utils.conversion.HexConverter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Hashing {

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
        return HexConverter.bytesAsHexString(encodedHash);
    }

    /**
     * Computes an SHA-256 hash of the given string.
     *
     * @param z String to hash
     * @return Hash
     */
    public static byte[] sha256hash(String z) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(z.getBytes(UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("Could not compute hash", ex);
        }
    }

}
