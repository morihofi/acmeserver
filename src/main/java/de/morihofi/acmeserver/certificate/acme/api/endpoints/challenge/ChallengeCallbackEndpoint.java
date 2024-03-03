package de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.objects.ACMEChallengeResponse;
import de.morihofi.acmeserver.certificate.acme.challenges.DNSChallenge;
import de.morihofi.acmeserver.certificate.acme.challenges.HTTPChallenge;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEConnectionErrorException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A handler endpoint for processing challenge callbacks.
 */
public class ChallengeCallbackEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger(getClass());


    /**
     * Constructs a NewNonce handler with the specified ACME provisioner.
     *
     * @param provisioner The ACME provisioner to use for generating nonces.
     */
    public ChallengeCallbackEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String challengeId = ctx.pathParam("challengeId");
        String challengeType = ctx.pathParam("challengeType"); //dns-01 or http-01

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce());

        // Check if challenge is valid
        ACMEIdentifier identifier = Database.getACMEIdentifierByChallengeId(challengeId);

        // Check signature and nonce
        performSignatureAndNonceCheck(ctx, identifier.getOrder().getAccount(), acmeRequestBody);

        {
            //TODO: Let user decide to enable compatibility mode or not for cases like this

            // This is a bugfix for Win ACME (https://www.win-acme.com/),
            // cause it sends HTTP_01 instead of HTTP-01 which is RFC compliant
            challengeType = challengeType.toLowerCase().replace("http_01","http-01");
            challengeType = challengeType.toLowerCase().replace("dns_01","dns-01"); //Same goes for DNS-01
        }

        boolean isWildcardDomain = false;
        String nonWildcardDomain = identifier.getDataValue();
        if (nonWildcardDomain.startsWith("*.")) {
            nonWildcardDomain = nonWildcardDomain.substring(2); // Remove wildcard part for validation
            isWildcardDomain = true;
        }

        if (!"dns-01".equals(challengeType) && isWildcardDomain) {
            throw new ACMEMalformedException("DNS-01 method is only valid for non wildcard domains");
        }

        boolean challengePassed;
        String possibleErrorReasonIfFailed;
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

        log.info("Validating ownership of host {}", nonWildcardDomain);
        if (challengePassed) {
            // Mark challenge as passed
            Database.passChallenge(challengeId);
        } else {
            log.error("Throwing API error: Host verification failed with method {}", challengeType);
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
