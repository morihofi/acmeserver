package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.AuthzResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.Challenge;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuthzOwnershipEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger(getClass());

    /**
     * @param provisioner Provisioner instance
     */
    public AuthzOwnershipEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String authorizationId = ctx.pathParam("authorizationId");

        ctx.header("Content-Type", "application/jose+json");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.status(200);

        ACMEIdentifier identifier = Database.getACMEIdentifierByAuthorizationId(authorizationId);

        // Not found handling
        if (identifier == null) {
            log.error("Throwing API error: For the requested authorization id {} was no identifier found", authorizationId);
            throw new ACMEMalformedException("For the requested authorization id was no identifier found");
        }

        // Check signature and nonce
        performSignatureAndNonceCheck(ctx, identifier.getOrder().getAccount(), acmeRequestBody);


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




    /**
     * Creates a challenge object of the specified type for the given ACME identifier.
     *
     * @param type       The type of challenge to create.
     * @param identifier The ACME identifier for which the challenge is created.
     * @return A challenge object with the specified type, URL, token, and status if verified.
     */
    private Challenge createChallenge(String type, ACMEIdentifier identifier) {
        Challenge challenge = new Challenge();
        challenge.setType(type);
        challenge.setUrl(getProvisioner().getApiURL() + "/acme/chall/" + identifier.getChallengeId() + "/" + type);
        challenge.setToken(identifier.getAuthorizationToken());
        if (identifier.isVerified()) {
            challenge.setStatus("valid");
            challenge.setValidated(DateTools.formatDateForACME(identifier.getVerifiedTime()));
        }
        return challenge;
    }
}
