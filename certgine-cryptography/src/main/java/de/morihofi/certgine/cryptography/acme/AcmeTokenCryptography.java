/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.acme;

import de.morihofi.certgine.utils.base64.Base64Tools;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import java.security.PublicKey;

/**
 * Utility class for ACME token cryptography, providing methods to compute thumbprints and key authorizations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

}
