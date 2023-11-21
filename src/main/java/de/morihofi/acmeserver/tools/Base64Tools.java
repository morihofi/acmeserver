package de.morihofi.acmeserver.tools;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Tools {
    public static String decodeBase64(String encodedString){
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

        return decodedString;
    }

    public static String encodeBase64(String originalInput){
        return encodeBase64(originalInput.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeBase64(byte[] originalInput){
        String encodedString = Base64.getEncoder().encodeToString(originalInput);
        return encodedString;
    }
}
