package de.morihofi.acmeserver.tools.crypto;

import de.morihofi.acmeserver.tools.base64.Base64Tools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import java.lang.invoke.MethodHandles;
import java.security.PublicKey;

public class AcmeTokenCryptography {
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    private AcmeTokenCryptography(){}

    /**
     * Computes a thumbprint of the given public key.
     *
     * @param key {@link PublicKey} to get the thumbprint of
     * @return Thumbprint of the key
     */
    public static byte[] thumbprint(PublicKey key) {
        try {
            var jwk = PublicJsonWebKey.Factory.newPublicJwk(key);
            return jwk.calculateThumbprint("SHA-256");
        } catch (JoseException ex) {
            throw new IllegalArgumentException("Bad public key", ex);
        }
    }

    /**
     * Computes the key authorization for the given token.
     * <p>
     * The default is {@code token + '.' + base64url(jwkThumbprint)}. Subclasses may
     * override this method if a different algorithm is used.
     *
     * @param token Token to be used
     * @param pk Public Key to be used
     * @return Key Authorization string for that token
     */
    public static String keyAuthorizationFor(String token, PublicKey pk) {
        return token + '.' + Base64Tools.base64UrlEncode(thumbprint(pk));
    }


}
