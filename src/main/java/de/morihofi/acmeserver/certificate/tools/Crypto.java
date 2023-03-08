package de.morihofi.acmeserver.certificate.tools;

import java.security.MessageDigest;
import java.security.SecureRandom;

public class Crypto {

    public static String createNonce() {
        String nonce = "";

        try {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            String randomNum = String.valueOf(prng.nextInt());

            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] result = sha.digest(randomNum.getBytes());
            nonce = hexEncode(result);
        } catch (Exception e) {
        }

        return nonce;
    }
    public static String hexEncode(byte[] aInput) {
        StringBuilder result = new StringBuilder();

        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };

        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }

        return result.toString();
    }

}
