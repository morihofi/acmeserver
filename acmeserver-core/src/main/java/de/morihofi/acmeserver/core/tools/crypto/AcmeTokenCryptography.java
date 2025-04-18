/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.tools.crypto;

import de.morihofi.acmeserver.core.tools.base64.Base64Tools;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import java.lang.invoke.MethodHandles;
import java.security.PublicKey;

/**
 * Utility class for ACME token cryptography, providing methods to compute thumbprints and key authorizations.
 */
public class AcmeTokenCryptography {

    /**
     * Computes a thumbprint of the given public key.
     *
     * @param key {@link PublicKey} to get the thumbprint of.
     * @return Thumbprint of the key as a byte array.
     */
    public static byte[] thumbprint(PublicKey key) {
        try {
            PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(key);
            return jwk.calculateThumbprint("SHA-256");
        } catch (JoseException ex) {
            throw new IllegalArgumentException("Bad public key", ex);
        }
    }

    /**
     * Computes the key authorization for the given token.
     * <p>
     * The default is {@code token + '.' + base64url(jwkThumbprint)}. Subclasses may override this method if a different algorithm is used.
     *
     * @param token Token to be used.
     * @param pk    Public Key to be used.
     * @return Key Authorization string for that token.
     */
    public static String keyAuthorizationFor(String token, PublicKey pk) {
        return token + '.' + Base64Tools.base64UrlEncode(thumbprint(pk));
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private AcmeTokenCryptography() {}
}
