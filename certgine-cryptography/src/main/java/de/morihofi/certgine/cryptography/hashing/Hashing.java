/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.hashing;


import de.morihofi.certgine.utils.conversion.HexConverter;
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
