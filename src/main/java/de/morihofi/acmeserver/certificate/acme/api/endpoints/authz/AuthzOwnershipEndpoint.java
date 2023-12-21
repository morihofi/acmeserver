package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.AuthzResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.Challenge;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuthzOwnershipEndpoint implements Handler {

    private final Provisioner provisioner;
    private final Logger log = LogManager.getLogger(getClass());
    private final Gson gson;

    public AuthzOwnershipEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
        this.gson = new Gson();
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String authorizationId = ctx.pathParam("authorizationId");

        ctx.header("Content-Type", "application/jose+json");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.status(200);

        ACMEIdentifier identifier = Database.getACMEIdentifierByAuthorizationId(authorizationId);
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        // Not found handling
        if (identifier == null) {
            log.error("Throwing API error: For the requested authorization id was no identifier found");
            throw new ACMEMalformedException("For the requested authorization id was no identifier found");
        }

        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, identifier.getOrder().getAccount(), gson);
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        boolean isWildcardDomain = identifier.getDataValue().startsWith("*.");
        String nonWildcardDomain = identifier.getDataValue();
        if (isWildcardDomain) {
            nonWildcardDomain = nonWildcardDomain.substring(2); // Remove wildcard part for validation
        }

        Identifier idObj = new Identifier();
        idObj.setType(identifier.getType());
        idObj.setValue(nonWildcardDomain);

        List<Challenge> challenges = new ArrayList<>();

        // HTTP-01 Challenge only for non-wildcard domains
        if (!isWildcardDomain) {
            challenges.add(createChallenge("http-01", identifier));
        }

        // DNS-01 Challenge
        challenges.add(createChallenge("dns-01", identifier));

        AuthzResponse response = new AuthzResponse();
        response.setStatus(identifier.isVerified() ? "valid" : "pending");
        response.setExpires(DateTools.formatDateForACME(new Date()));
        response.setIdentifier(idObj);
        response.setChallenges(challenges);

        ctx.result(gson.toJson(response));
    }


    private Challenge createChallenge(String type, ACMEIdentifier identifier) {
        Challenge challenge = new Challenge();
        challenge.setType(type);
        challenge.setUrl(provisioner.getApiURL() + "/acme/chall/" + identifier.getChallengeId() + "/" + type);
        challenge.setToken(identifier.getAuthorizationToken());
        if (identifier.isVerified()) {
            challenge.setStatus("valid");
            challenge.setValidated(DateTools.formatDateForACME(identifier.getVerifiedTime()));
        }
        return challenge;
    }
}
