/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.regex;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainValidatorTest {

    @Test
    void testValidDomain() {
        assertTrue(DomainValidator.isValidDomain("example.com", false));
    }

    @Test
    void testValidSubdomain() {
        assertTrue(DomainValidator.isValidDomain("sub.example.com", false));
    }

    @Test
    void testValidHostname() {
        assertTrue(DomainValidator.isValidDomain("localhost", false));
    }

    @Test
    void testValidDomainWithNumbers() {
        assertTrue(DomainValidator.isValidDomain("123domain.com", false));
    }

    @Test
    void testValidDomainWithHyphens() {
        assertTrue(DomainValidator.isValidDomain("my-domain.com", false));
    }

    @Test
    void testValidDomainWithWildcard() {
        assertTrue(DomainValidator.isValidDomain("*.example.com", true));
    }

    @Test
    void testValidSingleLabelHostname() {
        assertTrue(DomainValidator.isValidDomain("hostname", false));
    }

    @Test
    void testInvalidDomainStartingWithHyphen() {
        assertFalse(DomainValidator.isValidDomain("-example.com", false));
    }

    @Test
    void testInvalidDomainEndingWithHyphen() {
        assertFalse(DomainValidator.isValidDomain("example-.com", false));
    }

    @Test
    void testInvalidDomainDoubleDot() {
        assertFalse(DomainValidator.isValidDomain("example..com", false));
    }

    @Test
    void testInvalidDomainTrailingDot() {
        assertFalse(DomainValidator.isValidDomain("example.com.", false));
    }

    @Test
    void testInvalidTldTooShort() {
        assertFalse(DomainValidator.isValidDomain("example.c", false));
    }

    @Test
    void testInvalidWildcardWithoutPermission() {
        assertFalse(DomainValidator.isValidDomain("*.example.com", false));
    }

    @Test
    void testInvalidDomainWithUnderscore() {
        assertFalse(DomainValidator.isValidDomain("exam_ple.com", false));
    }

    @Test
    void testEmptyString() {
        assertFalse(DomainValidator.isValidDomain("", false));
    }

    @Test
    void testOnlyDot() {
        assertFalse(DomainValidator.isValidDomain(".", false));
    }

    @Test
    void testDomainTooLongLabel() {
        String longLabel = "a".repeat(64); // exceeds 63 chars
        assertFalse(DomainValidator.isValidDomain(longLabel + ".com", false));
    }

    // IP ADDRESS TESTS (Should be invalid as domains)

    @Test
    void testIPv4Address() {
        assertFalse(DomainValidator.isValidDomain("192.168.1.1", false));
    }

    @Test
    void testIPv6Address() {
        assertFalse(DomainValidator.isValidDomain("2001:0db8:85a3:0000:0000:8a2e:0370:7334", false));
    }

    // NULL / EXCEPTION TESTS

    @Test
    @SuppressWarnings("ConstantConditions")
    void testNullDomainThrowsException() {
        assertThrows(NullPointerException.class, () -> DomainValidator.isValidDomain(null, false));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testNullDomainWithWildcardThrowsException() {
        assertThrows(NullPointerException.class, () -> DomainValidator.isValidDomain(null, true));
    }
}
