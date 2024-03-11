package de.morihofi.acmeserver.certificate.acme.api.endpoints.authz;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.AuthzResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.objects.ChallengeResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.challenges.AcmeChallengeType;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifierChallenge;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AuthzOwnershipEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger(getClass());

    /**
     * @param provisioner Provisioner instance
     */
    public AuthzOwnershipEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String authorizationId = ctx.pathParam("authorizationId");

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.status(200);

        ACMEOrderIdentifier identifier = Database.getACMEIdentifierByAuthorizationId(authorizationId);

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

        List<ACMEOrderIdentifierChallenge> acmeChallenges = new ArrayList<>();


       if(!identifier.isHasChallengesGenerated()){
           //Challenges were not generated, so let's do that

           if (idObj.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.DNS) {
               // HTTP-01 Challenge only for non-wildcard domains
               if (!isWildcardDomain) {
                   acmeChallenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier));

                   //This is just a placeholder for the currently unsupported TLS-ALPN Challenge
                   //challenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));

               }

               // DNS-01 Challenge
               acmeChallenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.DNS_01, identifier));
           } else if (idObj.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.IP) {
               // HTTP-01 Challenge is the only allowed for IP addresses
               acmeChallenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.HTTP_01, identifier));

               //This is just a placeholder for the currently unsupported TLS-ALPN Challenge
               //challenges.add(new ACMEOrderIdentifierChallenge(AcmeChallengeType.TLS_ALPN_01, identifier));
           }


           //Save in database
           try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
               Transaction transaction = session.beginTransaction();

               for (ACMEOrderIdentifierChallenge challenge : acmeChallenges){
                   session.persist(challenge);
               }

               identifier.setHasChallengesGenerated(true); //to prevent infinite challenge generation
               session.merge(identifier);

               transaction.commit();
           } catch (Exception e) {
               log.error("Unable to persist ACME Identifier Challenges for authorization id {} (for account {})", authorizationId, identifier.getOrder().getAccount().getAccountId(), e);
               throw new ACMEServerInternalException("Unable to create new ACME Order");
           }

       }else {
           //Challenges were already generated, so load these instead to generate response
           acmeChallenges.addAll(identifier.getChallenges());
       }


        List<ChallengeResponse> challengeResponses = new ArrayList<>();

        for (ACMEOrderIdentifierChallenge acmeChallenge : acmeChallenges){
            //Add to response
            challengeResponses.add(createChallengeResponse(acmeChallenge.getChallengeType(), acmeChallenge));
        }







        AuthzResponse response = new AuthzResponse();
        response.setStatus(identifier.getChallengeStatus().getRfcName());
        response.setExpires(DateTools.formatDateForACME(new Date()));
        response.setIdentifier(idObj);
        response.setChallenges(challengeResponses);

        ctx.result(gson.toJson(response));
    }


    /**
     * Creates a challenge object of the specified type for the given ACME identifier.
     *
     * @param type       The type of challenge to create.
     * @param identifierChallenge The ACME identifier for which the challenge is created.
     * @return A challenge object with the specified type, URL, token, and status if verified.
     */
    private ChallengeResponse createChallengeResponse(AcmeChallengeType type, ACMEOrderIdentifierChallenge identifierChallenge) {
        ChallengeResponse challengeResponse = new ChallengeResponse();
        challengeResponse.setType(type.getName());
        challengeResponse.setUrl(getProvisioner().getApiURL() + "/acme/chall/" + identifierChallenge.getChallengeId() + "/" + type.getName());
        challengeResponse.setToken(identifierChallenge.getAuthorizationToken());
        if (identifierChallenge.getStatus() == AcmeStatus.VALID) {
            challengeResponse.setStatus(AcmeStatus.VALID.getRfcName());
            challengeResponse.setValidated(DateTools.formatDateForACME(identifierChallenge.getVerifiedTime()));
        }else {
            challengeResponse.setStatus(identifierChallenge.getStatus().getRfcName());
        }
        return challengeResponse;
    }


}
