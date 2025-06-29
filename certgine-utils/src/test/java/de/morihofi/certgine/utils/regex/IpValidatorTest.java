/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.regex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IpValidatorTest {

    // --- Tests for isIPv4Address ---

    @Test
    void testValidIPv4Addresses() {
        assertTrue(IpValidator.isIPv4Address("192.168.0.1"));
        assertTrue(IpValidator.isIPv4Address("0.0.0.0"));
        assertTrue(IpValidator.isIPv4Address("255.255.255.255"));
        assertTrue(IpValidator.isIPv4Address("127.0.0.1"));
    }

    @Test
    void testInvalidIPv4Addresses() {
        assertFalse(IpValidator.isIPv4Address("256.256.256.256"));
        assertFalse(IpValidator.isIPv4Address("192.168.0"));
        assertFalse(IpValidator.isIPv4Address("192.168.0.256"));
        assertFalse(IpValidator.isIPv4Address("abc.def.ghi.jkl"));
        assertFalse(IpValidator.isIPv4Address("..."));
    }

    // --- Tests for isIPv6Address ---

    @Test
    void testValidIPv6Addresses() {
        assertTrue(IpValidator.isIPv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertTrue(IpValidator.isIPv6Address("2001:db8::1"));
        assertTrue(IpValidator.isIPv6Address("::1"));
        assertTrue(IpValidator.isIPv6Address("::"));
    }

    @Test
    void testInvalidIPv6Addresses() {
        assertFalse(IpValidator.isIPv6Address("2001::85a3::8a2e"));
        assertFalse(IpValidator.isIPv6Address("2001:db8:85a3"));
        assertFalse(IpValidator.isIPv6Address("12345::"));
        assertFalse(IpValidator.isIPv6Address("GGGG:0db8:85a3:0000:0000:8a2e:0370:7334"));
    }

    // --- Tests for isIpAddress (combined logic) ---

    @Test
    void testIsIpAddressWithValidInputs() {
        assertTrue(IpValidator.isIpAddress("8.8.8.8"));
        assertTrue(IpValidator.isIpAddress("::1"));
        assertTrue(IpValidator.isIpAddress("2001:db8::1"));
    }

    @Test
    void testIsIpAddressWithInvalidInputs() {
        assertFalse(IpValidator.isIpAddress("999.999.999.999"));
        assertFalse(IpValidator.isIpAddress("hello world"));
        assertFalse(IpValidator.isIpAddress("1.2.3"));
    }

    // --- Edge Cases ---

    @Test
    void testEmptyAndEdgeCases() {
        assertFalse(IpValidator.isIpAddress(""));
        assertFalse(IpValidator.isIpAddress(" "));
        assertFalse(IpValidator.isIpAddress(":::")); // Too many colons
    }

    // --- Null Case (expect exception due to @NonNull) ---

    @Test
    @SuppressWarnings("ConstantConditions")
    void testNullInputThrowsException() {
        assertThrows(NullPointerException.class, () -> IpValidator.isIpAddress(null));
        assertThrows(NullPointerException.class, () -> IpValidator.isIPv4Address(null));
        assertThrows(NullPointerException.class, () -> IpValidator.isIPv6Address(null));
    }
}
