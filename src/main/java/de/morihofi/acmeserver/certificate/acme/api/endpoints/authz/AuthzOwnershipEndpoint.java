/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.AuthzResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.ChallengeResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.challenges.AcmeChallengeType;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifierChallenge;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Endpoint for handling authorization ownership challenges in the ACME protocol.
 */
public class AuthzOwnershipEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger instance for logging ACME authorization ownership activities.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Constructs a new endpoint for handling authorization ownership challenges.
     *
     * @param provisioner    The provisioner instance.
     * @param serverInstance The server instance.
     */
    public AuthzOwnershipEndpoint(Provisioner provisioner, ServerInstance serverInstance) {
        super(provisioner, serverInstance);
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
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String authorizationId = ctx.pathParam("authorizationId");

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce(getServerInstance()));
        ctx.status(200);

        ACMEOrderIdentifier identifier = ACMEOrderIdentifier.getACMEIdentifierByAuthorizationId(authorizationId, getServerInstance());

        // Not found handling
        if (identifier == null) {
            LOG.error("Throwing API error: For the requested authorization id {} was no identifier found", authorizationId);
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

        List<ACMEOrderIdentifierChallenge> acmeChallenges = new ArrayList<>();

        if (!identifier.isHasChallengesGenerated()) {
            // Challenges were not generated, so let's do that

            if (idObj.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.DNS) {
                // HTTP-01 Challenge only for non-wildcard domains
                if (!isWildcardDomain) {
                    acmeChallenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier));

                    // This is just a placeholder for the currently unsupported TLS-ALPN Challenge
                    // challenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));

                }

                // DNS-01 Challenge
                acmeChallenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.DNS_01, identifier));
            } else if (idObj.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.IP) {
                // HTTP-01 Challenge is the only allowed for IP addresses
                acmeChallenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier));

                // This is just a placeholder for the currently unsupported TLS-ALPN Challenge
                // challenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));
            }

            // Save in database
            try (Session session = Objects.requireNonNull(getServerInstance().getHibernateUtil().getSessionFactory()).openSession()) {
                Transaction transaction = session.beginTransaction();

                for (ACMEOrderIdentifierChallenge challenge : acmeChallenges) {
                    session.persist(challenge);
                }

                identifier.setHasChallengesGenerated(true); // to prevent infinite challenge generation
                session.merge(identifier);

                transaction.commit();
            } catch (Exception e) {
                LOG.error("Unable to persist ACME Identifier Challenges for authorization id {} (for account {})", authorizationId,
                        identifier.getOrder().getAccount().getAccountId(), e);
                throw new ACMEServerInternalException("Unable to create new ACME Order");
            }
        } else {
            // Challenges were already generated, so load these instead to generate response
            acmeChallenges.addAll(identifier.getChallenges());
        }

        List<ChallengeResponse> challengeResponses = new ArrayList<>();

        for (ACMEOrderIdentifierChallenge acmeChallenge : acmeChallenges) {
            // Add to response
            challengeResponses.add(createChallengeResponse(acmeChallenge.getChallengeType(), acmeChallenge));
        }

        AuthzResponse response = new AuthzResponse();
        response.setStatus(identifier.getChallengeStatus().getRfcName());
        response.setExpires(DateTools.formatDateForACME(new Date()));
        response.setIdentifier(idObj);
        response.setChallenges(challengeResponses);

        ctx.json(response);
    }

    /**
     * Creates a challenge response object of the specified type for the given ACME identifier challenge.
     *
     * @param type                The type of challenge to create.
     * @param identifierChallenge The ACME identifier challenge for which the response is created.
     * @return A challenge response object with the specified type, URL, token, and status.
     */
    private ChallengeResponse createChallengeResponse(AcmeChallengeType type, ACMEOrderIdentifierChallenge identifierChallenge) {
        ChallengeResponse challengeResponse = new ChallengeResponse();
        challengeResponse.setType(type.getName());
        challengeResponse.setUrl(
                getProvisioner().getApiURL() + "/acme/chall/" + identifierChallenge.getChallengeId() + "/" + type.getName());
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
