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

package de.morihofi.acmeserver.core.api.acme.api.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.acmeserver.core.api.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.core.api.acme.api.endpoints.challenge.objects.ACMEChallengeResponse;
import de.morihofi.acmeserver.core.api.acme.challenges.ChallengeResult;
import de.morihofi.acmeserver.core.api.acme.challenges.DNSChallenge;
import de.morihofi.acmeserver.core.api.acme.challenges.HTTPChallenge;
import de.morihofi.acmeserver.core.api.acme.api.objects.ACMERequestBody;
import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.HttpNonces;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEConnectionErrorException;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEResourceNotFoundException;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.utils.datetime.DateTools;
import de.morihofi.acmeserver.core.helper.http.HttpHeaderUtil;
import de.morihofi.acmeserver.types.events.EventBus;
import de.morihofi.acmeserver.types.events.BeforeChallengeEvent;
import de.morihofi.acmeserver.types.events.AfterChallengeEvent;
import de.morihofi.acmeserver.types.api.acme.challenge.AcmeChallengeType;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * A handler endpoint for processing challenge callbacks.
 */
@Slf4j
public class ChallengeCallbackEndpoint extends AbstractAcmeEndpoint {

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner and server instance.
     *
     * @param serverInstance The server instance.
     */
    public ChallengeCallbackEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

    @Override
    public void handleRequest(@NotNull Context ctx, @NotNull AcmeProvisioner provisioner, @NotNull Gson gson, @NotNull ACMERequestBody acmeRequestBody) throws Exception {
        String challengeId = ctx.pathParam("challengeId");
        String challengeType = ctx.pathParam("challengeType"); // dns-01 or http-01

        // Validate challengeId and challengeType
        if (challengeId.isEmpty()) {
            throw new ACMEMalformedException("Challenge ID is missing or empty");
        }
        if (!"dns-01".equals(challengeType) && !"http-01".equals(challengeType)) {
            throw new ACMEMalformedException("Challenge type must be either 'dns-01' or 'http-01'");
        }


        // Set response headers
        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));

        // Check if challenge is valid
        AcmeOrderIdentifierChallenge identifierChallenge = AcmeOrderIdentifierChallenge.getACMEIdentifierChallenge(challengeId, getServerInstance());

        if (identifierChallenge == null){
            throw new ACMEResourceNotFoundException("Challenge not found");
        }

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

        // move challenge into processing state before performing validation
        AcmeOrderIdentifierChallenge.markChallenge(AcmeStatus.PROCESSING, challengeId, getServerInstance());

        AcmeChallengeType typeEnum = "http-01".equals(challengeType) ? AcmeChallengeType.HTTP_01 : AcmeChallengeType.DNS_01;
        getServerInstance().getEventBus().publish(new BeforeChallengeEvent(typeEnum, challengeId));

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
        if (result.successful()) {
            // Mark challenge as passed
            AcmeOrderIdentifierChallenge.passChallenge(challengeId, getServerInstance());
        } else {
            AcmeOrderIdentifierChallenge.failChallenge(challengeId, getServerInstance());

            log.error("Throwing API error: Host verification failed with method {}", challengeType);
            throw new ACMEConnectionErrorException(result.errorReason());
        }

        getServerInstance().getEventBus().publish(new AfterChallengeEvent(typeEnum, challengeId, result.successful()));

        // Reload identifier, e.g., host has validated
        identifierChallenge = AcmeOrderIdentifierChallenge.getACMEIdentifierChallenge(challengeId, getServerInstance());

        // Creating response object
        ACMEChallengeResponse response = new ACMEChallengeResponse();
        response.setType(challengeType);
        if (identifierChallenge.getStatus() == AcmeStatus.VALID) {
            response.setStatus(AcmeStatus.VALID.getRfcName());
            response.setValidated(DateTools.formatDateForACME(identifierChallenge.getVerifiedTime()));
        } else {
            response.setStatus(AcmeStatus.PENDING.getRfcName());
        }
        response.setUrl(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/chall/" + challengeId + "/" + challengeType);
        response.setToken(identifierChallenge.getAuthorizationToken());

        // "Up"-Link header is required for certbot
        ctx.header("Link", HttpHeaderUtil.buildLinkHeaderValue(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/authz/" + identifierChallenge.getIdentifier().getAuthorizationId(), "up"));

        ctx.json(response);
    }
}
