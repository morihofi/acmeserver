package de.morihofi.certgine.cryptography.acme;

import de.morihofi.certgine.utils.base64.Base64Tools;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class AcmeTokenCryptographyTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    @DisplayName("thumbprint returns SHA-256 hash")
    void testThumbprint() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        byte[] thumb = AcmeTokenCryptography.thumbprint(kp.getPublic());
        assertEquals(32, thumb.length);
    }

    @Test
    @DisplayName("keyAuthorizationFor concatenates token and thumbprint")
    void testKeyAuthorizationFor() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        String token = "testtoken";

        byte[] thumb = AcmeTokenCryptography.thumbprint(kp.getPublic());
        String expected = token + '.' + Base64Tools.base64UrlEncode(thumb);

        assertEquals(expected, AcmeTokenCryptography.keyAuthorizationFor(token, kp.getPublic()));
    }
}
