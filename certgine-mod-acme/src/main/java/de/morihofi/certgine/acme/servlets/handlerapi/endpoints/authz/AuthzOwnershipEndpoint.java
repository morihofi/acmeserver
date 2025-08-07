/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.certgine.acme.servlets.handlerapi.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.authz.objects.AuthzResponse;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.authz.objects.ChallengeResponse;
import de.morihofi.certgine.acme.servlets.handlerapi.objects.ACMERequestBody;
import de.morihofi.certgine.acme.types.api.AcmeChallengeType;
import de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier;
import de.morihofi.certgine.acme.types.entities.AcmeHttpNonce;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.acme.util.AcmeTimeHelper;
import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.exception.exceptions.ACMEResourceNotFoundException;
import de.morihofi.certgine.types.exception.exceptions.ACMEServerInternalException;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.utils.base64.Base64Tools;
import de.morihofi.certgine.utils.conversion.HexConverter;
import de.morihofi.certgine.utils.datetime.TimeTools;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Endpoint for handling authorization ownership challenges in the ACME protocol.
 */
@Slf4j
public class AuthzOwnershipEndpoint extends AbstractAcmeEndpoint {

    private final Clock clock;
    /**
     * A supplier for generating a random challenge ID in hexadecimal format.
     * This is used to create unique identifiers for each challenge.
     */
    private final Supplier<String> challengeIdSupplier = () -> HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId());
    /**
     * A supplier for generating a base64 URL-encoded authorization token.
     * This token is used in the ACME challenge process to verify ownership of the domain.
     */
    private final Supplier<String> authorizationTokenBase64UrlSupplier = () -> Base64Tools.base64UrlEncode(HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId()).getBytes(StandardCharsets.UTF_8));

    /**
     * Constructs a new endpoint for handling authorization ownership challenges.
     *
     * @param moduleInstance The module instance.
     * @param clock          Clock used for time calculations.
     */
    public AuthzOwnershipEndpoint(CertgineModuleInstance moduleInstance, Clock clock) {
        super(moduleInstance);
        this.clock = clock;
    }

    /**
     * Constructs a new endpoint using the system UTC clock.
     *
     * @param moduleInstance The module instance.
     */
    public AuthzOwnershipEndpoint(CertgineModuleInstance moduleInstance) {
        this(moduleInstance, Clock.systemUTC());
    }

    /**
     * Handles the request for authorization ownership challenges.
     *
     * @param ctx             The Javalin context.
     * @param provisioner     The provisioner instance.
     * @param gson            The Gson instance for JSON processing.
     * @param acmeRequestBody The ACME request body.
     * @throws Exception If an error occurs while handling the request.
     */
    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
        String authorizationId = ctx.pathParam("authorizationId");

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", AcmeHttpNonce.createNonce(getModuleInstance().getModule().getServerInstance()));
        ctx.status(200);

        de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier identifier = de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier.getACMEIdentifierByAuthorizationId(authorizationId, getModuleInstance().getModule().getServerInstance());

        // Not found handling
        if (identifier == null) {
            log.error("Throwing API error: For the requested authorization id {} was no identifier found", authorizationId);
            throw new ACMEResourceNotFoundException("For the requested authorization id was no identifier found");
        }

        // Check signature and nonce
        performSignatureAndNonceCheck(ctx, identifier.getOrder().getAccount(), acmeRequestBody);

        boolean isWildcardDomain = identifier.getDataValue().startsWith("*.");
        String nonWildcardDomain = identifier.getDataValue();
        if (isWildcardDomain) {
            nonWildcardDomain = nonWildcardDomain.substring(2); // Remove wildcard part for validation
        }

        AcmeOrderIdentifier idObj = new AcmeOrderIdentifier();
        idObj.setType(identifier.getType());
        idObj.setValue(nonWildcardDomain);

        List<AcmeOrderIdentifierChallenge> acmeChallenges = new ArrayList<>();

        if (!identifier.isHasChallengesGenerated()) {
            // Challenges were not generated, so let's do that

            if (idObj.getTypeAsEnumConstant() == AcmeOrderIdentifier.IDENTIFIER_TYPE.DNS) {
                // HTTP-01 Challenge only for non-wildcard domains


                if (!isWildcardDomain) {
                    acmeChallenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier, challengeIdSupplier.get(), authorizationTokenBase64UrlSupplier.get()));

                    // This is just a placeholder for the currently unsupported TLS-ALPN Challenge
                    // challenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));

                }

                // DNS-01 Challenge
                acmeChallenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.DNS_01, identifier, challengeIdSupplier.get(), authorizationTokenBase64UrlSupplier.get()));
            } else if (idObj.getTypeAsEnumConstant() == AcmeOrderIdentifier.IDENTIFIER_TYPE.IP) {
                // HTTP-01 Challenge is the only allowed for IP addresses
                acmeChallenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier, challengeIdSupplier.get(), authorizationTokenBase64UrlSupplier.get()));

                // This is just a placeholder for the currently unsupported TLS-ALPN Challenge
                // challenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));
            }

            // Save in database
            try (Session session = getModuleInstance().getModule().getServerInstance().getDatabaseSession()) {
                Transaction transaction = session.beginTransaction();

                // Persist generated challenges in database
                for (AcmeOrderIdentifierChallenge challenge : acmeChallenges) {
                    session.persist(challenge);
                }

                identifier.setHasChallengesGenerated(true); // to prevent infinite challenge generation
                session.merge(identifier);

                transaction.commit();
            } catch (Exception e) {
                log.error("Unable to persist ACME Identifier Challenges for authorization id {} (for account {})", authorizationId,
                        identifier.getOrder().getAccount().getAccountId(), e);
                throw new ACMEServerInternalException("Unable to create new ACME Order");
            }
        } else {
            // Challenges were already generated, so load these instead to generate response
            acmeChallenges.addAll(identifier.getChallenges());
        }

        List<ChallengeResponse> challengeResponses = new ArrayList<>();

        for (AcmeOrderIdentifierChallenge acmeChallenge : acmeChallenges) {
            // Add to response
            challengeResponses.add(createChallengeResponse(acmeChallenge.getChallengeType(), acmeChallenge, provisioner));
        }

        AuthzResponse response = new AuthzResponse();
        response.setStatus(identifier.getChallengeStatus().getRfcName());
        response.setExpires(AcmeTimeHelper.formatInstantForAcme(getAuthorizationExpiration(identifier)));
        response.setIdentifier(idObj);
        response.setChallenges(challengeResponses);

        ctx.json(response);
    }

    /**
     * Returns the expiration timestamp for the provided authorization identifier.
     *
     * @param identifier The ACME order identifier.
     * @return The expiration {@link Instant} of the authorization.
     */
    Instant getAuthorizationExpiration(@NonNull de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier identifier) {
        return identifier.getOrder().getExpires();
    }

    /**
     * Creates a challenge response object of the specified type for the given ACME identifier challenge.
     *
     * @param type                The type of challenge to create.
     * @param identifierChallenge The ACME identifier challenge for which the response is created.
     * @return A challenge response object with the specified type, URL, token, and status.
     */
    private ChallengeResponse createChallengeResponse(AcmeChallengeType type, AcmeOrderIdentifierChallenge identifierChallenge, AcmeProvisioner p) {
        ChallengeResponse challengeResponse = new ChallengeResponse();
        challengeResponse.setType(type.getName());
        challengeResponse.setUrl(
                p.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/chall/" + identifierChallenge.getChallengeId() + "/" + type.getName());
        challengeResponse.setToken(identifierChallenge.getAuthorizationToken());
        if (identifierChallenge.getStatus() == AcmeStatus.VALID) {
            challengeResponse.setStatus(AcmeStatus.VALID.getRfcName());
            challengeResponse.setValidated(AcmeTimeHelper.formatInstantForAcme(identifierChallenge.getVerifiedTime()));
        } else {
            challengeResponse.setStatus(identifierChallenge.getStatus().getRfcName());
        }
        return challengeResponse;
    }
}
