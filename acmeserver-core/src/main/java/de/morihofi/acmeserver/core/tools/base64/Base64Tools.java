package de.morihofi.acmeserver.core.tools.base64;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for doing Base64 stuff
 */
public class Base64Tools {

    /**
     * Base64Url encoder (without padding) instance
     */
    private static final java.util.Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * Decodes a Base64-encoded string into its original string representation.
     *
     * @param encodedString The Base64-encoded string to decode.
     * @return The decoded string.
     */
    public static String decodeBase64(String encodedString) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Encodes a string into a Base64-encoded string.
     *
     * @param originalInput The string to encode.
     * @return The Base64-encoded string.
     */
    public static String encodeBase64(String originalInput) {
        return encodeBase64(originalInput.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encodes a byte array into a Base64-encoded string.
     *
     * @param originalInput The byte array to encode.
     * @return The Base64-encoded string.
     */
    public static String encodeBase64(byte[] originalInput) {
        return Base64.getEncoder().encodeToString(originalInput);
    }

    /**
     * Decodes a Base64 URL-safe encoded string into a UTF-8 encoded string.
     *
     * @param stringToDecode The Base64 URL-safe encoded string to decode.
     * @return The decoded UTF-8 encoded string.
     */
    public static String decodeBase64URL(String stringToDecode) {
        // Decoding URl
        return new String(decodeBase64URLAsBytes(stringToDecode), StandardCharsets.UTF_8);
    }

    /**
     * Decodes a Base64 URL-safe encoded string into a byte array.
     *
     * @param stringToDecode The Base64 URL-safe encoded string to decode.
     * @return The decoded byte array.
     */
    public static byte[] decodeBase64URLAsBytes(String stringToDecode) {
        // Getting decoder
        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
        return decoder.decode(stringToDecode);
    }

    /**
     * Encodes a string into a Base64 URL-safe representation.
     *
     * @param stringToEncode The string to encode.
     * @return The Base64 URL-safe encoded string.
     */
    public static String encodeBase64URL(String stringToEncode) {

        // Getting encoder
        java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder();

        // Encoding URL
        return encoder.encodeToString(stringToEncode.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 encodes the given byte array, using URL style encoding.
     *
     * @param data byte array to base64 encode
     * @return base64 encoded string
     */
    public static String base64UrlEncode(byte[] data) {
        return URL_ENCODER.encodeToString(data);
    }
}
