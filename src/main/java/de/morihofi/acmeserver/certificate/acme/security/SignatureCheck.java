package de.morihofi.acmeserver.certificate.acme.security;

import com.google.gson.Gson;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadSignatureAlgorithmException;
import de.morihofi.acmeserver.tools.CertTools;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class SignatureCheck {

    public static final Logger log = LogManager.getLogger(SignatureCheck.class);

    public static void checkSignature(Context ctx, ACMEAccount account, Gson gson) throws NoSuchAlgorithmException, SignatureException, IOException, InvalidKeySpecException, InvalidKeyException, NoSuchProviderException {
        checkSignature(ctx, account.getAccountId(), gson);
    }

    public static void checkSignature(Context ctx, String accountId, Gson gson) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException, IOException, InvalidKeySpecException, NoSuchProviderException {
        ACMERequestBody requestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);



        String protectedHeader = requestBody.getProtected();
        String payload = requestBody.getPayload();
        String signature = requestBody.getSignature();


        // Decode the protected header and parse it to JSON
        byte[] decodedProtectedHeaderBytes = Base64.getUrlDecoder().decode(protectedHeader);
        String decodedProtectedHeader = new String(decodedProtectedHeaderBytes);
        JSONObject protectedHeaderObj = new JSONObject(decodedProtectedHeader);

        // Ermittle den Algorithmus aus dem protected Header
        String alg = protectedHeaderObj.getString("alg");


        // Obtain the client's public key
        ACMEAccount account = Database.getAccount(accountId);
        PublicKey publicKey = CertTools.convertToPublicKey(account.getPublicKeyPEM());

        // Decode the signature
        byte[] decodedSignature = Base64.getUrlDecoder().decode(signature);


        // Verify the signature

        // Erstelle eine Signaturinstanz basierend auf dem Algorithmus
        Signature sig;
        switch (alg) {
            case "RS256":
                sig = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
                break;
            case "ES256":
                sig = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME);

                decodedSignature = CertTools.convertRawToDerSignatureECDSA(decodedSignature);
                break;
            // Fügen Sie hier weitere Algorithmen hinzu, falls notwendig
            default:
                throw new NoSuchAlgorithmException("Unsupported signature algorithm: " + alg);
        }

        // Überprüfe die Signatur
        sig.initVerify(publicKey);
        sig.update((protectedHeader + "." + payload).getBytes(StandardCharsets.UTF_8));

        boolean isSignatureValid = sig.verify(decodedSignature);

        if (!isSignatureValid) {
            // Signature verification failed
            log.error("Signature verification failed for account \"" + accountId + "\"");
            throw new ACMEBadSignatureAlgorithmException("Signature does not match");
        }



    }

    public static String getAccountIdFromProtectedKID(JSONObject protectedHeader){
        String prefix = "/acme/acct/";
        String kid = protectedHeader.getString("kid");


        int startIndex = kid.indexOf(prefix);
        if (startIndex != -1) {
            // Add the length of the prefix to the start index to find the start of the UUID.
            String uuid = kid.substring(startIndex + prefix.length());
            return uuid;
        } else {
            return null;
        }
    }

    public static PublicKey convertJWKToPublicKey(JSONObject jwkObj) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidParameterSpecException, NoSuchProviderException {
        String kty = jwkObj.getString("kty");
        if ("RSA".equals(kty)) {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwkObj.getString("n")));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwkObj.getString("e")));

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory factory = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            return factory.generatePublic(spec);
        } else if ("EC".equals(kty)) {
            String crv = jwkObj.getString("crv");
            BigInteger x = new BigInteger(1, Base64.getUrlDecoder().decode(jwkObj.getString("x")));
            BigInteger y = new BigInteger(1, Base64.getUrlDecoder().decode(jwkObj.getString("y")));

            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            parameters.init(new ECGenParameterSpec(crv));
            ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

            ECPoint ecPoint = new ECPoint(x, y);
            ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, ecParameters);
            KeyFactory factory = KeyFactory.getInstance("EC");
            return factory.generatePublic(spec);
        }

        throw new IllegalArgumentException("Unsupported key type: " + kty);
    }


    public static String getAccountIdFromProtectedKID(String protectedJsonString) {
        return getAccountIdFromProtectedKID(new JSONObject(protectedJsonString));
    }
}
