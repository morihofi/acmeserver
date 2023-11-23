package de.morihofi.acmeserver.tools.crypto;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class HashingTest {

    @Test
    void hashStringSHA256() throws NoSuchAlgorithmException {
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", Hashing.hashStringSHA256("test"));
    }
}