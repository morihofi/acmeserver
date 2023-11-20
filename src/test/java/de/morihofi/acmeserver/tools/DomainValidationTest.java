package de.morihofi.acmeserver.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainValidationTest {

    @Test
    @DisplayName("Check validity of domain names")
    void isValidDomain() {
        assertTrue(DomainValidation.isValidDomain("example.com"));
        assertTrue(DomainValidation.isValidDomain("sub.domain.co.uk"));
        assertTrue(DomainValidation.isValidDomain("hostname.internal"));
        assertTrue(DomainValidation.isValidDomain("host-name.internal"));
        assertTrue(DomainValidation.isValidDomain("hostname"));
        assertTrue(DomainValidation.isValidDomain("host.name"));
        assertTrue(DomainValidation.isValidDomain("host-name"));

        assertFalse(DomainValidation.isValidDomain("192.168.0.1"));
        assertFalse(DomainValidation.isValidDomain("::1"));
        assertFalse(DomainValidation.isValidDomain("invalid_domain"));
        assertFalse(DomainValidation.isValidDomain("*.example.com"));
    }
}