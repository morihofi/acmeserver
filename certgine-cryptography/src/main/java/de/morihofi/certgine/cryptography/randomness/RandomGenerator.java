/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.randomness;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.SecureRandom;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_BIT_LENGTH = 130;

    /**
     * Generates a cryptographically strong random identifier using the default bit length (130 bits).
     *
     * @return A random, unique identifier as a String.
     */
    public static BigInteger generateRandomId() {
        return generateRandomId(DEFAULT_BIT_LENGTH);
    }

    /**
     * Generates a cryptographically strong random identifier with the specified bit length.
     *
     * @param bitLength The bit length of the random number. Must be greater than 0.
     * @return A random, unique identifier as a String.
     * @throws IllegalArgumentException if bitLength is less than or equal to 0.
     */
    public static BigInteger generateRandomId(int bitLength) {
        if (bitLength <= 0) {
            throw new IllegalArgumentException("Bit length must be greater than 0");
        }
        return new BigInteger(bitLength, SECURE_RANDOM);
    }
}
