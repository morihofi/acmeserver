package de.morihofi.certgine.cryptography.pem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class PemUtilTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("convertToPem and convertPemToByteArray roundtrip")
    void testConvertToPemRoundtrip() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(1024);
        KeyPair kp = kpg.generateKeyPair();

        String pem = PemUtil.convertToPem(kp.getPublic());
        byte[] decoded = PemUtil.convertPemToByteArray(pem);
        assertArrayEquals(kp.getPublic().getEncoded(), decoded);
    }
}
