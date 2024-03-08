package de.morihofi.acmeserver.tools.regex;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainAndIpValidationTest {

    @Test
    @DisplayName("Check validity of domain names")
    void isValidDomain() {
        assertTrue(DomainAndIpValidation.isValidDomain("example.com", false));
        assertTrue(DomainAndIpValidation.isValidDomain("sub.domain.co.uk", false));
        assertTrue(DomainAndIpValidation.isValidDomain("hostname.internal", false));
        assertTrue(DomainAndIpValidation.isValidDomain("host-name.internal", false));
        assertTrue(DomainAndIpValidation.isValidDomain("hostname", false));
        assertTrue(DomainAndIpValidation.isValidDomain("host.name", false));
        assertTrue(DomainAndIpValidation.isValidDomain("host-name", false));
        assertTrue(DomainAndIpValidation.isValidDomain("*.example.com", true));


        assertFalse(DomainAndIpValidation.isValidDomain("192.168.0.1", true));
        assertFalse(DomainAndIpValidation.isValidDomain("192.168.0.1", false));
        assertFalse(DomainAndIpValidation.isValidDomain("::1", false));
        assertFalse(DomainAndIpValidation.isValidDomain("invalid_domain", false));
        assertFalse(DomainAndIpValidation.isValidDomain("*.example.com", false));
    }


    @Test
    @DisplayName("IPV4: Check validity of IPv4 addresses")
    void testValidIPv4Addresses() {
        assertTrue(DomainAndIpValidation.isIpAddress("127.0.0.1"));
        assertTrue(DomainAndIpValidation.isIpAddress("192.168.1.1"));
        assertTrue(DomainAndIpValidation.isIpAddress("255.255.255.255"));
    }

    @Test
    @DisplayName("IPV4: Check invalidity of IPv4 addresses")
    void testInvalidIPv4Addresses() {
        assertFalse(DomainAndIpValidation.isIpAddress("127.0.0.256")); // Invalid area
        assertFalse(DomainAndIpValidation.isIpAddress("192.168.1.256")); // Invalid area
        assertFalse(DomainAndIpValidation.isIpAddress("300.255.255.255")); // Invalid area
        assertFalse(DomainAndIpValidation.isIpAddress("192.168.1")); // Missing octets
    }

    @Test
    @DisplayName("IPv4: Valid address with leading zeros")
    void isValidIpAddressIPv4LeadingZeros() {
        assertTrue(DomainAndIpValidation.isIpAddress("192.168.001.001"));
    }

    @Test
    @DisplayName("IPv4: Borderline case of valid address")
    void isValidIpAddressIPv4Borderline() {
        assertTrue(DomainAndIpValidation.isIpAddress("255.255.255.255"));
    }

    @Test
    @DisplayName("IPv4: Invalid address with negative number")
    void isInvalidIpAddressIPv4Negative() {
        assertFalse(DomainAndIpValidation.isIpAddress("192.168.-1.1"));
    }

    @Test
    @DisplayName("IPv4: Invalid address with numbers exceeding 255")
    void isInvalidIpAddressIPv4Exceed255() {
        assertFalse(DomainAndIpValidation.isIpAddress("256.100.100.100"));
    }

    @Test
    @DisplayName("IPv6: Valid full notation")
    void isValidIpAddressIPv6Full() {
        assertTrue(DomainAndIpValidation.isIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
    }

    @Test
    @DisplayName("IPv6: Valid compressed notation")
    void isValidIpAddressIPv6Compressed() {
        assertTrue(DomainAndIpValidation.isIpAddress("2001:db8:85a3::8a2e:370:7334"));
    }

    @Test
    @DisplayName("IPv6: Valid loopback address")
    void isValidIpAddressIPv6Loopback() {
        assertTrue(DomainAndIpValidation.isIpAddress("::1"));
    }

    @Test
    @DisplayName("IPv6: Invalid with extra segments")
    void isInvalidIpAddressIPv6ExtraSegments() {
        assertFalse(DomainAndIpValidation.isIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334:1234"));
    }

    @Test
    @DisplayName("IPv6: Invalid with segment too long")
    void isInvalidIpAddressIPv6SegmentTooLong() {
        assertFalse(DomainAndIpValidation.isIpAddress("2001:0db85a3:0000:0000:8a2e:0370:7334"));
    }

    @Test
    @DisplayName("IPv6: Invalid mixed with IPv4 exceeding valid range")
    void isInvalidIpAddressIPv6MixedIPv4Exceed() {
        assertFalse(DomainAndIpValidation.isIpAddress("::ffff:256.256.256.256"));
    }
}