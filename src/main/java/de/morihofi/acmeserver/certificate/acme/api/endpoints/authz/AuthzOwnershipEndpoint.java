package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.Crypto;
import de.morihofi.acmeserver.tools.DateTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

public class AuthzOwnershipEndpoint implements Handler {

    private Provisioner provisioner;
    private final Logger log = LogManager.getLogger(getClass());

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner.
     *
     * @param provisioner The ACME provisioner to use for generating nonces.
     */
    public AuthzOwnershipEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String authorizationId = ctx.pathParam("authorizationId");

        ctx.header("Content-Type", "application/jose+json");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.status(200);


        Gson gson = new Gson();
        ACMEIdentifier identifier = Database.getACMEIdentifierByAuthorizationId(authorizationId);
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        //Check signature
        SignatureCheck.checkSignature(ctx, identifier.getOrder().getAccount(), gson);
        //Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());


        //Not found
        if (identifier == null) {
            log.error("Throwing API error: The requested authorization id was not found");
            throw new ACMEMalformedException("The requested authorization id was not found");
        }

        JSONObject identifierObj = new JSONObject();
        identifierObj.put("type", identifier.getType());
        identifierObj.put("value", identifier.getDataValue());


        JSONArray challengeArr = new JSONArray();

        JSONObject challengeHTTP = new JSONObject();
        challengeHTTP.put("type", "http-01"); //
        challengeHTTP.put("url", provisioner.getApiURL() + "/acme/chall/" + identifier.getChallengeId()); //Challenge callback URL, called after created token on server
        challengeHTTP.put("token", identifier.getAuthorizationToken());
        if (identifier.isVerified()) {
            challengeHTTP.put("status", "valid");
            challengeHTTP.put("validated", DateTools.formatDateForACME(identifier.getVerifiedTime()));
        }
        challengeArr.put(challengeHTTP);


        JSONObject returnObj = new JSONObject();

        if (identifier.isVerified()) {
            returnObj.put("status", "valid");
        } else {
            returnObj.put("status", "pending");
        }
        returnObj.put("expires", DateTools.formatDateForACME(new Date()));
        returnObj.put("identifier", identifierObj);
        returnObj.put("challenges", challengeArr);


        ctx.result(returnObj.toString());
    }
}
