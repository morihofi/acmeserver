package de.morihofi.acmeserver.tools.base64;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Base64ToolsTest {

    @Test
    void testEncodeAndDecodeBase64() {
        String originalString = "Hello, World!";
        String encodedString = Base64Tools.encodeBase64(originalString);
        String decodedString = Base64Tools.decodeBase64(encodedString);

        assertEquals(originalString, decodedString, "The decoded string should match the original.");
    }

    @Test
    void testEncodeAndDecodeBase64WithBytes() {
        String originalString = "Hello, World!";
        byte[] originalBytes = originalString.getBytes(StandardCharsets.UTF_8);
        String encodedString = Base64Tools.encodeBase64(originalBytes);
        String decodedString = Base64Tools.decodeBase64(encodedString);

        assertEquals(originalString, decodedString, "The decoded string should match the original.");
    }

    @Test
    void testEncodeAndDecodeBase64URL() {
        String originalString = "Hello, World! /?=+";
        String encodedString = Base64Tools.encodeBase64URL(originalString);
        String decodedString = Base64Tools.decodeBase64URL(encodedString);

        assertEquals(originalString, decodedString, "The decoded string should match the original.");
    }

    @Test
    void testDecodeBase64URLAsBytes() {
        String originalString = "Hello, World! /?=+";
        String encodedString = Base64Tools.encodeBase64URL(originalString);
        byte[] decodedBytes = Base64Tools.decodeBase64URLAsBytes(encodedString);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

        assertEquals(originalString, decodedString, "The decoded string should match the original.");
    }

}