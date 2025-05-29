package de.morihofi.acmeserver.utils.conversion;

import lombok.NonNull;

import java.math.BigInteger;

public class HexConverter {


    @NonNull
    public static String bigIntegerAsHexString(@NonNull BigInteger input){
        return input.toString(32);
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array to convert.
     * @return The hexadecimal representation of the byte array.
     */
    @NonNull
    public static String bytesAsHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b & 0xff));
        }
        return hexString.toString();
    }
}
