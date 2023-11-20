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

    private final Provisioner provisioner;
    private final Logger log = LogManager.getLogger(getClass());

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

        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, identifier.getOrder().getAccount(), gson);
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        // Not found handling
        if (identifier == null) {
            log.error("Throwing API error: The requested authorization id was not found");
            throw new ACMEMalformedException("The requested authorization id was not found");
        }
        boolean isWildcardDomain = identifier.getDataValue().startsWith("*.");
        String nonWildcardDomain = identifier.getDataValue();
        if (nonWildcardDomain.startsWith("*.")) {
            nonWildcardDomain = nonWildcardDomain.substring(2); // Remove wildcard part for validation
        }


        JSONObject identifierObj = new JSONObject();
        identifierObj.put("type", identifier.getType());
        identifierObj.put("value", nonWildcardDomain);

        JSONArray challengeArr = new JSONArray();



        //HTTP-01 Challenge does only work on non-wildcard domains
        if (!isWildcardDomain) {
            // HTTP-01 Challenge
            JSONObject challengeHTTP = createChallengeObj("http-01", identifier);
            challengeArr.put(challengeHTTP);
        }

        // DNS-01 Challenge
        JSONObject challengeDNS = createChallengeObj("dns-01", identifier);
        challengeArr.put(challengeDNS);

        JSONObject returnObj = new JSONObject();
        returnObj.put("status", identifier.isVerified() ? "valid" : "pending");
        returnObj.put("expires", DateTools.formatDateForACME(new Date()));
        returnObj.put("identifier", identifierObj);
        returnObj.put("challenges", challengeArr);

        ctx.result(returnObj.toString());
    }

    private JSONObject createChallengeObj(String type, ACMEIdentifier identifier) {
        JSONObject challenge = new JSONObject();
        challenge.put("type", type);
        challenge.put("url", provisioner.getApiURL() + "/acme/chall/" + identifier.getChallengeId() + "/" + type);
        challenge.put("token", identifier.getAuthorizationToken());
        if (identifier.isVerified()) {
            challenge.put("status", "valid");
            challenge.put("validated", DateTools.formatDateForACME(identifier.getVerifiedTime()));
        }
        return challenge;
    }
}
