package de.morihofi.certgine.utils.crypto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

/**
 * Utility class for generating and verifying TOTP codes as used by applications like Google Authenticator.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TotpUtil {
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;

    /**
     * Generates a random Base32 encoded secret suitable for TOTP authenticators.
     *
     * @return secret string
     */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        java.security.SecureRandom random = new java.security.SecureRandom();
        random.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Calculates the TOTP code for the given secret at the provided timestamp.
     *
     * @param secret   Base64 encoded secret
     * @param timestamp timestamp in seconds
     * @return code
     */
    public static String generateCode(String secret, long timestamp) {
        long counter = timestamp / TIME_STEP_SECONDS;
        byte[] key = Base64.getDecoder().decode(secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xff);
            counter >>= 8;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot generate TOTP", e);
        }
    }

    /**
     * Verifies the given TOTP code for the provided secret.
     *
     * @param secret secret used for code generation
     * @param code   code supplied by user
     * @return {@code true} if code is valid
     */
    public static boolean verifyCode(String secret, String code) {
        long now = Instant.now().getEpochSecond();
        for (int i = -1; i <= 1; i++) {
            String expected = generateCode(secret, now + i * TIME_STEP_SECONDS);
            if (expected.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
