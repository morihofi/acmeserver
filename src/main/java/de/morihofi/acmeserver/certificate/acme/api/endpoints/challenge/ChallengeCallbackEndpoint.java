package de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.objects.ACMEChallengeResponse;
import de.morihofi.acmeserver.certificate.acme.challenges.DNSChallenge;
import de.morihofi.acmeserver.certificate.acme.challenges.HTTPChallenge;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEConnectionErrorException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ChallengeCallbackEndpoint implements Handler {

    private final Provisioner provisioner;
    private final Logger log = LogManager.getLogger(getClass());
    private final Gson gson;

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner.
     *
     * @param provisioner The ACME provisioner to use for generating nonces.
     */
    public ChallengeCallbackEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
        this.gson = new Gson();
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String challengeId = ctx.pathParam("challengeId");
        String challengeType = ctx.pathParam("challengeType"); //dns-01 or http-01

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce());

        // Check if challenge is valid
        ACMEIdentifier identifier = Database.getACMEIdentifierByChallengeId(challengeId);

        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        // Check signature
        SignatureCheck.checkSignature(ctx, identifier.getOrder().getAccount(), gson);
        // Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        boolean isWildcardDomain = false;
        String nonWildcardDomain = identifier.getDataValue();
        if (nonWildcardDomain.startsWith("*.")) {
            nonWildcardDomain = nonWildcardDomain.substring(2); // Remove wildcard part for validation
            isWildcardDomain = true;
        }

        if (!"dns-01".equals(challengeType) && isWildcardDomain) {
            throw new ACMEMalformedException("DNS-01 method is only valid for non wildcard domains");
        }

        boolean challengePassed = false;
        String possibleErrorReasonIfFailed = null;
        switch (challengeType) {
            case "http-01" -> {
                challengePassed = HTTPChallenge.check(identifier.getAuthorizationToken(), identifier.getDataValue(), identifier.getOrder().getAccount());
                possibleErrorReasonIfFailed = "Unable to reach host \"" + identifier.getDataValue() + "\" or invalid token. Is the host reachable? Is the http server on port 80 running? If it is running, check your access logs";
            }
            case "dns-01" -> {
                challengePassed = DNSChallenge.check(identifier.getAuthorizationToken(), nonWildcardDomain, identifier.getOrder().getAccount());
                possibleErrorReasonIfFailed = "Unable to verify DNS TXT entry for host \"" + nonWildcardDomain + "\"";
            }
            default -> {
                log.error("Unsupported challenge type: " + challengeType);
                throw new ACMEConnectionErrorException("Unsupported challenge type: " + challengeType);
            }
        }

        log.info("Validating ownership of host \"" + nonWildcardDomain + "\"");
        if (challengePassed) {
            // Mark challenge as passed
            Database.passChallenge(challengeId);
        } else {
            log.error("Throwing API error: Host verification failed with method " + challengeType);
            throw new ACMEConnectionErrorException(possibleErrorReasonIfFailed);
            // TODO: Fail challenge in database
            //Database.failChallenge(challengeId);
        }

        // Reload identifier, e.g., host has validated
        identifier = Database.getACMEIdentifierByChallengeId(challengeId);

        // Creating response object
        ACMEChallengeResponse response = new ACMEChallengeResponse();
        response.setType(challengeType);
        if (identifier.isVerified()) {
            response.setStatus("valid");
            response.setVerified(DateTools.formatDateForACME(identifier.getVerifiedTime()));
        } else {
            response.setStatus("pending");
        }
        response.setUrl(provisioner.getApiURL() + "/acme/chall/" + challengeId + "/" + challengeType);
        response.setToken(identifier.getAuthorizationToken());

        // "Up"-Link header is required for certbot
        ctx.header("Link", "<" + provisioner.getApiURL() + "/acme/authz/" + identifier.getAuthorizationId() + ">;rel=\"up\"");
        ctx.result(gson.toJson(response));
    }
}
