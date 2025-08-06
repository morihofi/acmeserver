/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.randomness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGeneratorTest {

    @Test
    @DisplayName("generateRandomId uses default bit length")
    void testGenerateRandomIdDefault() {
        BigInteger id = RandomGenerator.generateRandomId();
        assertTrue(id.bitLength() <= 130 && id.bitLength() > 0);
    }

    @Test
    @DisplayName("generateRandomId uses custom bit length")
    void testGenerateRandomIdCustom() {
        BigInteger id = RandomGenerator.generateRandomId(64);
        assertTrue(id.bitLength() <= 64 && id.bitLength() > 0);
    }

    @Test
    @DisplayName("generateRandomId throws on invalid length")
    void testGenerateRandomIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> RandomGenerator.generateRandomId(0));
    }
}
