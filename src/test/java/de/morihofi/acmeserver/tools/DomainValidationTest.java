package de.morihofi.acmeserver.tools;

import de.morihofi.acmeserver.tools.regex.DomainValidation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainValidationTest {

    @Test
    @DisplayName("Check validity of domain names")
    void isValidDomain() {
        assertTrue(DomainValidation.isValidDomain("example.com", false));
        assertTrue(DomainValidation.isValidDomain("sub.domain.co.uk", false));
        assertTrue(DomainValidation.isValidDomain("hostname.internal", false));
        assertTrue(DomainValidation.isValidDomain("host-name.internal", false));
        assertTrue(DomainValidation.isValidDomain("hostname", false));
        assertTrue(DomainValidation.isValidDomain("host.name", false));
        assertTrue(DomainValidation.isValidDomain("host-name", false));
        assertTrue(DomainValidation.isValidDomain("*.example.com", true));

        assertFalse(DomainValidation.isValidDomain("192.168.0.1", false));
        assertFalse(DomainValidation.isValidDomain("::1", false));
        assertFalse(DomainValidation.isValidDomain("invalid_domain", false));
        assertFalse(DomainValidation.isValidDomain("*.example.com", false));
    }
}