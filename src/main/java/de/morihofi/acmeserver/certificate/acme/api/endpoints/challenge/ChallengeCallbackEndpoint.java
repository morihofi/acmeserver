package de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.objects.ACMEChallengeResponse;
import de.morihofi.acmeserver.certificate.acme.challenges.ChallengeResult;
import de.morihofi.acmeserver.certificate.acme.challenges.DNSChallenge;
import de.morihofi.acmeserver.certificate.acme.challenges.HTTPChallenge;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifierChallenge;
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
        ACMEOrderIdentifierChallenge identifierChallenge = Database.getACMEIdentifierChallenge(challengeId);

        assert identifierChallenge != null;

        // Check signature and nonce
        performSignatureAndNonceCheck(ctx, identifierChallenge.getIdentifier().getOrder().getAccount(), acmeRequestBody);

        boolean isWildcardDomain = false;
        String nonWildcardDomain = identifierChallenge.getIdentifier().getDataValue();
        if (nonWildcardDomain.startsWith("*.")) {
            nonWildcardDomain = nonWildcardDomain.substring(2); // Remove wildcard part for validation
            isWildcardDomain = true;
        }

        if (!"dns-01".equals(challengeType) && isWildcardDomain) {
            throw new ACMEMalformedException("DNS-01 method is only valid for non wildcard domains");
        }

        ChallengeResult result;
        switch (challengeType) {
            case "http-01" -> {
                result = HTTPChallenge.check(identifierChallenge.getAuthorizationToken(), identifierChallenge.getIdentifier().getDataValue(), identifierChallenge.getIdentifier().getOrder().getAccount());
           }
            case "dns-01" -> {
                result = DNSChallenge.check(identifierChallenge.getAuthorizationToken(), nonWildcardDomain, identifierChallenge.getIdentifier().getOrder().getAccount());
            }
            default -> {
                log.error("Unsupported challenge type: " + challengeType);
                throw new ACMEConnectionErrorException("Unsupported challenge type: " + challengeType);
            }
        }

        log.info("Validating ownership of host {}", nonWildcardDomain);
        if (result.isSuccessful()) {
            // Mark challenge as passed
            Database.passChallenge(challengeId);
        } else {
            log.error("Throwing API error: Host verification failed with method {}", challengeType);
            throw new ACMEConnectionErrorException(result.getErrorReason());
            // TODO: Fail challenge in database
            //Database.failChallenge(challengeId);
        }

        // Reload identifier, e.g., host has validated
        identifierChallenge = Database.getACMEIdentifierChallenge(challengeId);

        // Creating response object
        ACMEChallengeResponse response = new ACMEChallengeResponse();
        response.setType(challengeType);
        if (identifierChallenge.getStatus() == AcmeStatus.VALID) {
            response.setStatus(AcmeStatus.VALID.getRfcName());
            response.setVerified(DateTools.formatDateForACME(identifierChallenge.getVerifiedTime()));
        } else {
            response.setStatus(AcmeStatus.PENDING.getRfcName());
        }
        response.setUrl(provisioner.getApiURL() + "/acme/chall/" + challengeId + "/" + challengeType);
        response.setToken(identifierChallenge.getAuthorizationToken());

        // "Up"-Link header is required for certbot
        ctx.header("Link", "<" + provisioner.getApiURL() + "/acme/authz/" + identifierChallenge.getIdentifier().getAuthorizationId() + ">;rel=\"up\"");
        ctx.result(gson.toJson(response));
    }
}
