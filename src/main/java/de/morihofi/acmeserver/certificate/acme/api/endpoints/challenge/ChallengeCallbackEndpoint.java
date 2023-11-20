package de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.challenges.DNSChallenge;
import de.morihofi.acmeserver.certificate.acme.challenges.HTTPChallenge;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEConnectionErrorException;
import de.morihofi.acmeserver.tools.Crypto;
import de.morihofi.acmeserver.tools.DateTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class ChallengeCallbackEndpoint implements Handler {

    private final Provisioner provisioner;
    private final Logger log = LogManager.getLogger(getClass());

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner.
     *
     * @param provisioner The ACME provisioner to use for generating nonces.
     */
    public ChallengeCallbackEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String challengeId = ctx.pathParam("challengeId");
        String challengeType = ctx.pathParam("challengeType"); //dns-01 or http-01


        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce());

        // check if challenge is valid
        ACMEIdentifier identifier = Database.getACMEIdentifierByChallengeId(challengeId);

        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);


        //Check signature
        SignatureCheck.checkSignature(ctx, identifier.getOrder().getAccount(), gson);
        //Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());


        boolean challengePassed = false;
        String possibleErrorReasonIfFailed = "Unknown error";
        switch (challengeType) {
            case "http-01" -> {
                challengePassed = HTTPChallenge.check(identifier.getAuthorizationToken(), identifier.getDataValue());
                possibleErrorReasonIfFailed = "Unable to reach host \"" + identifier.getDataValue() + "\" or invalid token. Is the host reachable? Is the http server on port 80 running? If it is running, check your access logs";
            }
            case "dns-01" -> {
                challengePassed = DNSChallenge.check(identifier.getAuthorizationToken(), identifier.getDataValue(), identifier.getOrder().getAccount());
                possibleErrorReasonIfFailed = "Unable to verify DNS TXT entry for host \"" + identifier.getDataValue() + "\"";
            }
            default -> {
                log.error("Unsupported challenge type: " + challengeType);
                throw new ACMEConnectionErrorException("Unsupported challenge type: " + challengeType);
            }
        }


        log.info("Validating ownership of host \"" + identifier.getDataValue() + "\"");
        if (challengePassed) {
            //mark challenge has passed
            Database.passChallenge(challengeId);
        } else {
            log.error("Throwing API error: Host verification failed with method" + challengeType);
            throw new ACMEConnectionErrorException(possibleErrorReasonIfFailed);

            // TODO: Fail challenge in database
            //Database.failChallenge(challengeId);
        }


        //Reload identifier, e.g. host has validated
        identifier = Database.getACMEIdentifierByChallengeId(challengeId);


        JSONObject responseJSON = new JSONObject();
        responseJSON.put("type", "http-01");
        if (identifier.isVerified()) {
            responseJSON.put("status", "valid");
            responseJSON.put("verified", DateTools.formatDateForACME(identifier.getVerifiedTime()));
        } else {
            responseJSON.put("status", "pending");
        }


        //"Up"-Link header is required for certbot
        ctx.header("Link", "<" + provisioner.getApiURL() + "/acme/authz/" + identifier.getAuthorizationId() + ">;rel=\"up\"");
        responseJSON.put("url", provisioner.getApiURL() + "/acme/chall/" + challengeId + "/" + challengeType);
        responseJSON.put("token", identifier.getAuthorizationToken());

        ctx.result(responseJSON.toString());

    }
}
