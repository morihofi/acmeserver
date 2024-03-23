package de.morihofi.acmeserver.certificate.acme.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadSignatureAlgorithmException;
import de.morihofi.acmeserver.exception.exceptions.ACMEUnauthorizedException;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Provides methods for verifying the signatures of ACME requests.
 * This class includes functionality to validate request signatures against public keys associated with ACME accounts,
 * ensuring the authenticity and integrity of the requests.
 */
public class SignatureCheck {

    public static final Logger log = LogManager.getLogger(SignatureCheck.class);

    private SignatureCheck() {
    }

    /**
     * Verifies the signature of an ACME request using an ACME account's public key.
     * The method checks the signature against the public key to ensure the request's authenticity and integrity.
     *
     * @param ctx     The Javalin context containing the request data.
     * @param account The ACME account whose public key is used for signature verification.
     * @param gson    The Gson instance for JSON parsing.
     * @throws ACMEBadSignatureAlgorithmException If the signature does not match.
     */
    public static void checkSignature(Context ctx, ACMEAccount account, Gson gson) {
        checkSignature(ctx, account.getAccountId(), gson);
    }

    /**
     * Verifies the signature of an ACME request using an ACME account's public key.
     * The method checks the signature against the public key to ensure the request's authenticity and integrity.
     *
     * @param ctx       The Javalin context containing the request data.
     * @param accountId The ACME account id whose public key is used for signature verification.
     * @param gson      The Gson instance for JSON parsing.
     * @throws ACMEBadSignatureAlgorithmException If the signature does not match.
     */
    public static void checkSignature(Context ctx, String accountId, Gson gson) {
        ACMERequestBody requestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        String protectedHeader = requestBody.getProtected();
        String payload = requestBody.getPayload();
        String signature = requestBody.getSignature();

        // Combine protected header and payload into a complete JWS representation
        String serializedJws = protectedHeader + "." + payload + "." + signature;

        try {
            // Obtain the client's public key
            ACMEAccount account = Database.getAccount(accountId);

            if(account.isDeactivated()){
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
        }catch (JoseException | IOException | NoSuchAlgorithmException | NoSuchProviderException |
                InvalidKeySpecException ex){
            log.error("An exception occurred while verifying signature for account {}", accountId, ex);
            throw new ACMEBadSignatureAlgorithmException("An server side exception occurred while verifying signature");
        }
    }

    /**
     * Extracts the account ID from the "kid" (Key Identifier) in the protected header of an ACME request.
     *
     * @param protectedHeader The protected header of an ACME request as a JSON object.
     * @return The account ID extracted from the "kid," or null if not found.
     */
    public static String getAccountIdFromProtectedKID(JsonObject protectedHeader) {
        String prefix = "/acme/acct/";
        String kid = protectedHeader.get("kid").getAsString();


        int startIndex = kid.indexOf(prefix);
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
    public static String getAccountIdFromProtectedKID(String protectedJsonString) {
        return getAccountIdFromProtectedKID(JsonParser.parseString(protectedJsonString).getAsJsonObject());
    }
}
