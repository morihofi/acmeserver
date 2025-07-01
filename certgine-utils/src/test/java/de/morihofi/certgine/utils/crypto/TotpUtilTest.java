package de.morihofi.certgine.utils.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpUtilTest {
    @Test
    void testGenerateAndVerify() {
        String secret = TotpUtil.generateSecret();
        String code = TotpUtil.generateCode(secret, System.currentTimeMillis() / 1000);
        assertTrue(TotpUtil.verifyCode(secret, code));
    }
}
