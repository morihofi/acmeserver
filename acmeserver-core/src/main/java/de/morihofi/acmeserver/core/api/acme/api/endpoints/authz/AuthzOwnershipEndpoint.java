/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.api.acme.api.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.acmeserver.core.api.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.core.api.acme.api.endpoints.authz.objects.AuthzResponse;
import de.morihofi.acmeserver.core.api.acme.api.endpoints.authz.objects.ChallengeResponse;
import de.morihofi.acmeserver.cryptography.randomness.RandomGenerator;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.types.api.acme.challenge.AcmeChallengeType;
import de.morihofi.acmeserver.core.api.acme.api.objects.ACMERequestBody;

import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.database.entities.AcmeOrderIdentifierChallenge;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.HttpNonces;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.utils.base64.Base64Tools;
import de.morihofi.acmeserver.utils.conversion.HexConverter;
import de.morihofi.acmeserver.utils.datetime.DateTools;
import io.javalin.http.Context;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/**
 * Endpoint for handling authorization ownership challenges in the ACME protocol.
 */
@Slf4j
public class AuthzOwnershipEndpoint extends AbstractAcmeEndpoint {

    /**
     * Constructs a new endpoint for handling authorization ownership challenges.
     *
     * @param serverInstance The server instance.
     */
    public AuthzOwnershipEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

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
     * Handles the request for authorization ownership challenges.
     *
     * @param ctx             The Javalin context.
     * @param provisioner     The provisioner instance.
     * @param gson            The Gson instance for JSON processing.
     * @param acmeRequestBody The ACME request body.
     * @throws Exception If an error occurs while handling the request.
     */
    @Override
    public void handleRequest(@NotNull Context ctx, @NotNull AcmeProvisioner provisioner, @NotNull Gson gson, @NotNull ACMERequestBody acmeRequestBody) throws Exception {
        String authorizationId = ctx.pathParam("authorizationId");

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));
        ctx.status(200);

        AcmeOrderIdentifier identifier = AcmeOrderIdentifier.getACMEIdentifierByAuthorizationId(authorizationId, getServerInstance());

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

        List<AcmeOrderIdentifierChallenge> acmeChallenges = new ArrayList<>();

        if (!identifier.isHasChallengesGenerated()) {
            // Challenges were not generated, so let's do that

            if (idObj.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.DNS) {
                // HTTP-01 Challenge only for non-wildcard domains


                if (!isWildcardDomain) {
                    acmeChallenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier, challengeIdSupplier.get(), authorizationTokenBase64UrlSupplier.get()));

                    // This is just a placeholder for the currently unsupported TLS-ALPN Challenge
                    // challenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));

                }

                // DNS-01 Challenge
                acmeChallenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.DNS_01, identifier, challengeIdSupplier.get(), authorizationTokenBase64UrlSupplier.get()));
            } else if (idObj.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.IP) {
                // HTTP-01 Challenge is the only allowed for IP addresses
                acmeChallenges.add(new AcmeOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier, challengeIdSupplier.get(), authorizationTokenBase64UrlSupplier.get()));

                // This is just a placeholder for the currently unsupported TLS-ALPN Challenge
                // challenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));
            }

            // Save in database
            try (Session session = getServerInstance().getDatabaseSession()) {
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
        response.setExpires(DateTools.formatDateForACME(getAuthorizationExpiration(identifier)));
        response.setIdentifier(idObj);
        response.setChallenges(challengeResponses);

        ctx.json(response);
    }

    /**
     * Returns the expiration timestamp for the provided authorization identifier.
     *
     * @param identifier The ACME order identifier.
     * @return The expiration {@link Date} of the authorization.
     */
    Date getAuthorizationExpiration(@NotNull AcmeOrderIdentifier identifier) {
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
                p.getAcmeApiURL(getServerInstance()) + "/acme/chall/" + identifierChallenge.getChallengeId() + "/" + type.getName());
        challengeResponse.setToken(identifierChallenge.getAuthorizationToken());
        if (identifierChallenge.getStatus() == AcmeStatus.VALID) {
            challengeResponse.setStatus(AcmeStatus.VALID.getRfcName());
            challengeResponse.setValidated(DateTools.formatDateForACME(identifierChallenge.getVerifiedTime()));
        } else {
            challengeResponse.setStatus(identifierChallenge.getStatus().getRfcName());
        }
        return challengeResponse;
    }
}
