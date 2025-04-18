/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.certificate.acme.api.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.acmeserver.core.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.challenge.objects.ACMEChallengeResponse;
import de.morihofi.acmeserver.core.certificate.acme.challenges.ChallengeResult;
import de.morihofi.acmeserver.core.certificate.acme.challenges.DNSChallenge;
import de.morihofi.acmeserver.core.certificate.acme.challenges.HTTPChallenge;
import de.morihofi.acmeserver.core.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.core.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.core.database.AcmeStatus;
import de.morihofi.acmeserver.core.database.objects.ACMEOrderIdentifierChallenge;
import de.morihofi.acmeserver.core.exception.exceptions.ACMEConnectionErrorException;
import de.morihofi.acmeserver.core.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import de.morihofi.acmeserver.core.tools.crypto.Crypto;
import de.morihofi.acmeserver.core.tools.dateAndTime.DateTools;
import de.morihofi.acmeserver.core.tools.http.HttpHeaderUtil;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;


import java.lang.invoke.MethodHandles;

/**
 * A handler endpoint for processing challenge callbacks.
 */
@Slf4j
public class ChallengeCallbackEndpoint extends AbstractAcmeEndpoint {

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner and server instance.
     *
     * @param provisioner The ACME provisioner to use for generating nonces.
     * @param serverInstance The server instance.
     */
    public ChallengeCallbackEndpoint(Provisioner provisioner, ServerInstance serverInstance) {
        super(provisioner, serverInstance);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String challengeId = ctx.pathParam("challengeId");
        String challengeType = ctx.pathParam("challengeType"); // dns-01 or http-01

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce(getServerInstance()));

        // Check if challenge is valid
        ACMEOrderIdentifierChallenge identifierChallenge = ACMEOrderIdentifierChallenge.getACMEIdentifierChallenge(challengeId, getServerInstance());

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

        ChallengeResult result = switch (challengeType) {
            case "http-01" -> HTTPChallenge.check(
                    identifierChallenge.getAuthorizationToken(),
                    identifierChallenge.getIdentifier().getDataValue(),
                    identifierChallenge.getIdentifier().getOrder().getAccount(),
                    getServerInstance()
            );
            case "dns-01" -> DNSChallenge.check(
                    identifierChallenge.getAuthorizationToken(),
                    nonWildcardDomain,
                    identifierChallenge.getIdentifier().getOrder().getAccount(),
                    getServerInstance()
            );
            default -> {
                log.error("Unsupported challenge type: {}", challengeType);
                throw new ACMEConnectionErrorException("Unsupported challenge type: " + challengeType);
            }
        };

        log.info("Validating ownership of host {}", nonWildcardDomain);
        if (result.isSuccessful()) {
            // Mark challenge as passed
            ACMEOrderIdentifierChallenge.passChallenge(challengeId, getServerInstance());
        } else {
            log.error("Throwing API error: Host verification failed with method {}", challengeType);
            throw new ACMEConnectionErrorException(result.getErrorReason());
            // TODO: Fail challenge in database
            // Database.failChallenge(challengeId);
        }

        // Reload identifier, e.g., host has validated
        identifierChallenge = ACMEOrderIdentifierChallenge.getACMEIdentifierChallenge(challengeId, getServerInstance());

        // Creating response object
        ACMEChallengeResponse response = new ACMEChallengeResponse();
        response.setType(challengeType);
        if (identifierChallenge.getStatus() == AcmeStatus.VALID) {
            response.setStatus(AcmeStatus.VALID.getRfcName());
            response.setVerified(DateTools.formatDateForACME(identifierChallenge.getVerifiedTime()));
        } else {
            response.setStatus(AcmeStatus.PENDING.getRfcName());
        }
        response.setUrl(provisioner.getAcmeApiURL() + "/acme/chall/" + challengeId + "/" + challengeType);
        response.setToken(identifierChallenge.getAuthorizationToken());

        // "Up"-Link header is required for certbot
        ctx.header("Link", HttpHeaderUtil.buildLinkHeaderValue(provisioner.getAcmeApiURL() + "/acme/authz/" + identifierChallenge.getIdentifier().getAuthorizationId(), "up"));

        ctx.json(response);
    }
}
