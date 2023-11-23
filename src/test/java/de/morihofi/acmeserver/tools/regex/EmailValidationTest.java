package de.morihofi.acmeserver.tools.regex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidationTest {

    @Test
    public void testValidEmail() {
        assertTrue(EmailValidation.isValidEmail("test@example.com"));
        assertTrue(EmailValidation.isValidEmail("user@subdomain.domain.co.uk"));
        assertTrue(EmailValidation.isValidEmail("12345@example.co"));
    }

    @Test
    public void testInvalidEmail() {
        assertFalse(EmailValidation.isValidEmail("notAnEmail"));
        assertFalse(EmailValidation.isValidEmail("invalid@.com"));
        assertFalse(EmailValidation.isValidEmail("@invalid.com"));
        assertFalse(EmailValidation.isValidEmail("invalid@.com."));
    }
}