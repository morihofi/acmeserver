package de.morihofi.acmeserver.cryptography.keys;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class KeyHelperTest {

    @BeforeAll
    static void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("RSA private key returns RSA algorithm")
    void testRsaKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(2048);
        PrivateKey pk = kpg.generateKeyPair().getPrivate();
        assertEquals("SHA256withRSA", KeyHelper.getSignatureAlgorithmBasedOnKeyType(pk));
    }

    @Test
    @DisplayName("ECDSA private key returns ECDSA algorithm")
    void testEcdsaKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(256);
        PrivateKey pk = kpg.generateKeyPair().getPrivate();
        assertEquals("SHA256withECDSA", KeyHelper.getSignatureAlgorithmBasedOnKeyType(pk));
    }

    @Test
    @DisplayName("DSA private key returns DSA algorithm")
    void testDsaKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(1024);
        PrivateKey pk = kpg.generateKeyPair().getPrivate();
        assertEquals("SHA256withDSA", KeyHelper.getSignatureAlgorithmBasedOnKeyType(pk));
    }

    @Test
    @DisplayName("Ed25519 private key returns Ed25519 algorithm")
    void testEd25519Key() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", BouncyCastleProvider.PROVIDER_NAME);
        PrivateKey pk = kpg.generateKeyPair().getPrivate();
        assertEquals("Ed25519", KeyHelper.getSignatureAlgorithmBasedOnKeyType(pk));
    }

    @Test
    @DisplayName("Unsupported key throws exception")
    void testUnsupportedKey() {
        PrivateKey dummy = new PrivateKey() {
            @Override public String getAlgorithm() { return "DUMMY"; }
            @Override public String getFormat() { return null; }
            @Override public byte[] getEncoded() { return new byte[0]; }
        };
        assertThrows(IllegalArgumentException.class, () -> KeyHelper.getSignatureAlgorithmBasedOnKeyType(dummy));
    }
}
