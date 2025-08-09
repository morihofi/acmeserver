/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.conversion;

import lombok.NonNull;

import java.math.BigInteger;

/**
 * Utility class providing helper methods for converting numeric and byte data to
 * hexadecimal string representations.
 */
public class HexConverter {


    /**
     * Converts the supplied {@link BigInteger} into its hexadecimal string representation.
     *
     * @param input the {@link BigInteger} value to convert
     * @return the hexadecimal string representation of {@code input}
     */
    @NonNull
    public static String bigIntegerAsHexString(@NonNull BigInteger input) {
        return input.toString(32);
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array to convert.
     * @return The hexadecimal representation of the byte array.
     */
    @NonNull
    public static String bytesAsHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b & 0xff));
        }
        return hexString.toString();
    }
}
