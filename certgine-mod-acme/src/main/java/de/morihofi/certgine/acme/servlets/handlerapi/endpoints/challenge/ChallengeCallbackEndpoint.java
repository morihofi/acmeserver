/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.challenge;

import com.google.gson.Gson;
import de.morihofi.certgine.acme.challenges.ChallengeResult;
import de.morihofi.certgine.acme.challenges.DNSChallenge;
import de.morihofi.certgine.acme.challenges.HTTPChallenge;
import de.morihofi.certgine.acme.servlets.handlerapi.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.challenge.objects.ACMEChallengeResponse;
import de.morihofi.certgine.acme.servlets.handlerapi.objects.ACMERequestBody;
import de.morihofi.certgine.acme.types.api.AcmeChallengeType;
import de.morihofi.certgine.acme.types.entities.AcmeHttpNonce;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.acme.types.events.AfterChallengeEvent;
import de.morihofi.certgine.acme.types.events.BeforeChallengeEvent;
import de.morihofi.certgine.acme.util.AcmeTimeHelper;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.exception.exceptions.ACMEConnectionErrorException;
import de.morihofi.certgine.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.certgine.types.exception.exceptions.ACMEResourceNotFoundException;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.utils.datetime.TimeTools;
import de.morihofi.certgine.utils.http.HttpHeaderUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
    public ChallengeCallbackEndpoint(CertgineModuleInstance moduleInstance) {
        super(moduleInstance);
    }

    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
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
        ctx.header("Replay-Nonce", AcmeHttpNonce.createNonce(getModuleInstance().getModule().getServerInstance()));

        // Check if challenge is valid
        AcmeOrderIdentifierChallenge identifierChallenge = AcmeOrderIdentifierChallenge.getACMEIdentifierChallenge(challengeId, getModuleInstance().getModule().getServerInstance());

        if (identifierChallenge == null) {
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
        AcmeOrderIdentifierChallenge.markChallenge(AcmeStatus.PROCESSING, challengeId, getModuleInstance().getModule().getServerInstance());

        AcmeChallengeType typeEnum = "http-01".equals(challengeType) ? AcmeChallengeType.HTTP_01 : AcmeChallengeType.DNS_01;
        getModuleInstance().getModule().getServerInstance().getEventBus().publish(new BeforeChallengeEvent(typeEnum, challengeId));

        ChallengeResult result = switch (challengeType) {
            case "http-01" -> HTTPChallenge.check(
                    identifierChallenge.getAuthorizationToken(),
                    identifierChallenge.getIdentifier().getDataValue(),
                    identifierChallenge.getIdentifier().getOrder().getAccount(),
                    getModuleInstance().getModule().getServerInstance()
            );
            case "dns-01" -> DNSChallenge.check(
                    identifierChallenge.getAuthorizationToken(),
                    nonWildcardDomain,
                    identifierChallenge.getIdentifier().getOrder().getAccount(),
                    getModuleInstance().getModule().getServerInstance()
            );
            default -> {
                log.error("Unsupported challenge type: {}", challengeType);
                throw new ACMEConnectionErrorException("Unsupported challenge type: " + challengeType);
            }
        };

        log.info("Validating ownership of host {}", nonWildcardDomain);
        if (result.successful()) {
            // Mark challenge as passed
            AcmeOrderIdentifierChallenge.passChallenge(challengeId, getModuleInstance().getModule().getServerInstance());
        } else {
            AcmeOrderIdentifierChallenge.failChallenge(challengeId, getModuleInstance().getModule().getServerInstance());
        }

        getModuleInstance().getModule().getServerInstance().getEventBus().publish(new AfterChallengeEvent(typeEnum, challengeId, result));

        if (!result.successful()) {
            log.error("Throwing API error: Host verification failed with method {}", challengeType);
            throw new ACMEConnectionErrorException(result.errorReason());
        }

        // Reload identifier, e.g., host has validated
        identifierChallenge = AcmeOrderIdentifierChallenge.getACMEIdentifierChallenge(challengeId, getModuleInstance().getModule().getServerInstance());

        // Creating response object
        ACMEChallengeResponse response = new ACMEChallengeResponse();
        response.setType(challengeType);
        if (identifierChallenge.getStatus() == AcmeStatus.VALID) {
            response.setStatus(AcmeStatus.VALID.getRfcName());
            response.setValidated(AcmeTimeHelper.formatInstantForAcme(identifierChallenge.getVerifiedTime()));
        } else {
            response.setStatus(AcmeStatus.PENDING.getRfcName());
        }
        response.setUrl(provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/chall/" + challengeId + "/" + challengeType);
        response.setToken(identifierChallenge.getAuthorizationToken());

        // "Up"-Link header is required for certbot
        ctx.header("Link", HttpHeaderUtil.buildLinkHeaderValue(provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/authz/" + identifierChallenge.getIdentifier().getAuthorizationId(), "up"));

        ctx.json(response);
    }
}
