package de.morihofi.acmeserver.utils.regex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {

    @Test
    void testValidEmails() {
        assertTrue(EmailValidator.isValidEmail("test@example.com"));
        assertTrue(EmailValidator.isValidEmail("user.name+tag+sorting@example.co.uk"));
        assertTrue(EmailValidator.isValidEmail("user_name@example.org"));
        assertTrue(EmailValidator.isValidEmail("username123@example.io"));
    }

    @Test
    void testInvalidEmails() {
        assertFalse(EmailValidator.isValidEmail("plainaddress"));
        assertFalse(EmailValidator.isValidEmail("@missingusername.com"));
        assertFalse(EmailValidator.isValidEmail("username@.com"));
        assertFalse(EmailValidator.isValidEmail("username@com"));
        assertFalse(EmailValidator.isValidEmail("username@domain..com"));
        assertFalse(EmailValidator.isValidEmail("username@domain.c"));
        assertFalse(EmailValidator.isValidEmail("username@domain.toolongtld"));
        assertFalse(EmailValidator.isValidEmail("user..name@example.com"));
        assertFalse(EmailValidator.isValidEmail("user.@example.com"));
    }

    @Test
    void testEdgeCases() {
        assertThrows(NullPointerException.class, () -> {
            EmailValidator.isValidEmail(null);
        });
        assertFalse(EmailValidator.isValidEmail(""));
        assertFalse(EmailValidator.isValidEmail(" "));
    }
}