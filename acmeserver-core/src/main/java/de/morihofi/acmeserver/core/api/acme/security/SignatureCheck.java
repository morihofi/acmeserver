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

package de.morihofi.acmeserver.core.api.acme.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.core.api.acme.api.objects.ACMERequestBody;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEBadSignatureAlgorithmException;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEUnauthorizedException;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import io.javalin.http.Context;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 * Provides methods for verifying the signatures of ACME requests. This class includes functionality to validate request signatures against
 * public keys associated with ACME accounts, ensuring the authenticity and integrity of the requests.
 */
@Slf4j
public class SignatureCheck {

    /**
     * Verifies the signature of an ACME request using an ACME account's public key. The method checks the signature against the public key
     * to ensure the request's authenticity and integrity.
     *
     * @param ctx            The Javalin context containing the request data.
     * @param account        The ACME account whose public key is used for signature verification.
     * @param gson           The Gson instance for JSON parsing.
     * @param serverInstance The server instance containing necessary configurations and services.
     * @throws ACMEBadSignatureAlgorithmException If the signature does not match.
     */
    public static void checkSignature(@NonNull Context ctx, @NonNull AcmeAccount account, @NonNull Gson gson, @NonNull IServerInstance serverInstance) {
        checkSignature(ctx, account.getAccountId(), gson, serverInstance);
    }

    /**
     * Verifies the signature of an ACME request using an ACME account's public key. The method checks the signature against the public key
     * to ensure the request's authenticity and integrity.
     *
     * @param ctx            The Javalin context containing the request data.
     * @param accountId      The ACME account id whose public key is used for signature verification.
     * @param gson           The Gson instance for JSON parsing.
     * @param serverInstance The server instance containing necessary configurations and services.
     * @throws ACMEBadSignatureAlgorithmException If the signature does not match.
     */
    public static void checkSignature(@NonNull Context ctx, @NonNull String accountId, @NonNull Gson gson, @NonNull IServerInstance serverInstance) {
        try {
            ACMERequestBody requestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

            String protectedHeader = requestBody.getProtected();
            String payload = requestBody.getPayload();
            String signature = requestBody.getSignature();

            // Combine protected header and payload into a complete JWS representation
            String serializedJws = protectedHeader + "." + payload + "." + signature;

            // Obtain the client's public key
            AcmeAccount account = AcmeAccount.getAccount(accountId, serverInstance);

            if (account.isDeactivated()) {
                throw new ACMEUnauthorizedException("Account is deactivated");
            }

            PublicKey publicKey = PemUtil.readPublicKeyFromPem(account.getPublicKeyPEM());

            // Create a JsonWebSignature object and set the required parts
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(serializedJws);
            jws.setKey(publicKey);

            // Verify the signature
            boolean isSignatureValid = jws.verifySignature();

            if (!isSignatureValid) {
                // Signature verification failed
                log.error("Signature verification failed for account {}", accountId);
                throw new ACMEBadSignatureAlgorithmException("Signature does not match");
            }
        } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
            log.error("An exception occurred while verifying signature for account {}", accountId, ex);
            throw new ACMEBadSignatureAlgorithmException("An server side exception occurred while verifying signature");
        } catch (JsonParseException e) {
            log.error("Unable to parse request body from JSON", e);
            throw new ACMEMalformedException("Unable to parse request body from JSON");
        } catch (JoseException e) {
            log.error("Unable to verify the request signature", e);
            throw new ACMEMalformedException("Unable to verify the request signature");
        }
    }

    /**
     * Extracts the account ID from the "kid" (Key Identifier) in the protected header of an ACME request.
     *
     * @param protectedHeader The protected header of an ACME request as a JSON object.
     * @return The account ID extracted from the "kid," or null if not found.
     */
    public static String getAccountIdFromProtectedKID(@NonNull JsonObject protectedHeader) {
        final String prefix = "/acme/acct/";
        final String kid = protectedHeader.get("kid").getAsString();

        final int startIndex = kid.indexOf(prefix);
        if (startIndex != -1) {
            // Add the length of the prefix to the start index to find the start of the UUID.
            return kid.substring(startIndex + prefix.length());
        } else {
            return null;
        }
    }

    /**
     * Extracts the account ID from the "kid" (Key Identifier) in the protected header of an ACME request.
     *
     * @param protectedJsonString The protected header of an ACME request as a JSON string.
     * @return The account ID extracted from the "kid," or null if not found.
     */
    public static String getAccountIdFromProtectedKID(@NonNull String protectedJsonString) {
        return getAccountIdFromProtectedKID(JsonParser.parseString(protectedJsonString).getAsJsonObject());
    }

    /**
     * Private constructor to prevent object instantiation
     */
    private SignatureCheck() {
    }
}
