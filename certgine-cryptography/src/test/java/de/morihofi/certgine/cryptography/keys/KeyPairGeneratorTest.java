package de.morihofi.certgine.cryptography.keys;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class KeyPairGeneratorTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("generateRSAKeyPair returns RSA keys")
    void testGenerateRSAKeyPair() throws Exception {
        KeyPair kp = KeyPairGenerator.generateRSAKeyPair(2048, BouncyCastleProvider.PROVIDER_NAME);
        assertNotNull(kp.getPrivate());
        assertEquals("RSA", kp.getPrivate().getAlgorithm());
    }

    @Test
    @DisplayName("generateEcdsaKeyPair returns EC keys")
    void testGenerateEcdsaKeyPair() throws Exception {
        KeyPair kp = KeyPairGenerator.generateEcdsaKeyPair("prime256v1", BouncyCastleProvider.PROVIDER_NAME);
        assertNotNull(kp.getPrivate());
        assertEquals("ECDSA", kp.getPrivate().getAlgorithm());
    }
}
