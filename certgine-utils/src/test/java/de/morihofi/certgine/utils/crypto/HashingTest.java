package de.morihofi.certgine.utils.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class HashingTest {

    @Test
    void hashStringSHA256ReturnsExpectedDigest() throws NoSuchAlgorithmException {
        String input = "certgine";
        String expected = "8fcd661be063e37574cc82f88efd1b6150a44ad6b906be9aba28c40f83efcc0c";
        assertEquals(expected, Hashing.hashStringSHA256(input));
    }

    @Test
    void sha256hashMatchesMessageDigest() throws NoSuchAlgorithmException {
        String input = "certgine";
        byte[] expected = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(expected, Hashing.sha256hash(input));
    }

    @Test
    void sha256hashNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Hashing.sha256hash(null));
    }
}

