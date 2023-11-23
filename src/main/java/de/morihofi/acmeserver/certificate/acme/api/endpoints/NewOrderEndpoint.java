package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMERejectedIdentifierException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import de.morihofi.acmeserver.tools.regex.DomainValidation;
import de.morihofi.acmeserver.tools.email.SendMail;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class NewOrderEndpoint implements Handler {
    private final Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public NewOrderEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }


    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        //Parse request body
        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        ACMEAccount account = Database.getAccount(accountId);
        //Check if account exists
        if (account == null) {
            log.error("Throwing API error: Account \"" + accountId + "\" not found");
            throw new ACMEAccountNotFoundException("The account id was not found");
        }
        //Check signature
        SignatureCheck.checkSignature(ctx, accountId, gson);
        //Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        JSONArray identifiersArr = new JSONObject(acmeRequestBody.getDecodedPayload()).getJSONArray("identifiers");

        ArrayList<ACMEIdentifier> acmeIdentifiers = new ArrayList<>();

        for (int i = 0; i < identifiersArr.length(); i++) {
            JSONObject identifier = identifiersArr.getJSONObject(i);
            String type = identifier.getString("type");
            String value = identifier.getString("value");

            acmeIdentifiers.add(new ACMEIdentifier(type, value));
        }

        JSONObject returnObj = new JSONObject();

        // Create order in Database
        String orderId = UUID.randomUUID().toString();

        if (account.getEmails().size() == 0) {
            throw new ACMEInvalidContactException("This account doesn't have any E-Mail addresses. Please set at least one E-Mail address and try again.");
        }


        JSONArray respIdentifiersArr = new JSONArray();
        JSONArray respAuthorizationsArr = new JSONArray();

        ArrayList<ACMEIdentifier> acmeIdentifiersWithAuthorizationData = new ArrayList<>();

        for (ACMEIdentifier identifier : acmeIdentifiers) {

            if (!identifier.getType().equals("dns")) {
                log.error("Throwing API error: Unknown identifier type \"" + identifier.getType() + "\" for value \"" + identifier.getDataValue() + "\"");
                throw new ACMERejectedIdentifierException("Unknown identifier type \"" + identifier.getType() + "\" for value \"" + identifier.getDataValue() + "\"");
            }

            if(!DomainValidation.isValidDomain(identifier.getDataValue(), provisioner.isWildcardAllowed())){
                throw new ACMERejectedIdentifierException("Identifier \"" + identifier.getDataValue() + "\" is invalid (Wildcard allowed: " + provisioner.isWildcardAllowed() + ")");

            }

            if(!checkIfDomainIsAllowed(identifier.getDataValue())){
                throw new ACMERejectedIdentifierException("Domain identifier \"" + identifier.getDataValue() + "\" is not allowed");
            }

            JSONObject identifierObj = new JSONObject();
            identifierObj.put("type", identifier.getType());
            identifierObj.put("value", identifier.getDataValue());


            respIdentifiersArr.put(identifierObj);

            // Unique value for each domain
            String authorizationId = Crypto.generateRandomId();
            // Random authorization token
            String authorizationToken = Crypto.generateRandomId();
            // Unique challenge id
            String challengeId = Crypto.generateRandomId();
            // Unique certificate id
            String certificateId = Crypto.generateRandomId();


            identifier.setAuthorizationToken(authorizationToken);
            identifier.setAuthorizationId(authorizationId);
            identifier.setChallengeId(challengeId);
            identifier.setCertificateId(certificateId);

            acmeIdentifiersWithAuthorizationData.add(identifier);

            respAuthorizationsArr.put(provisioner.getApiURL() + "/acme/authz/" + authorizationId);


        }
        // Add authorizations to Database
        Database.createOrder(account, orderId, acmeIdentifiersWithAuthorizationData, provisioner.getProvisionerName());

        //Send E-Mail if order was created
        try {
            SendMail.sendMail(account.getEmails().get(0), "New ACME order created", "Hey there, <br> a new ACME order (" + orderId + ") for <i>" + acmeIdentifiers.get(0).getDataValue() + "</i> was created.");
        } catch (Exception ex) {
            log.error("Unable to send email", ex);
        }


        //TODO: Set better Date/Time
        returnObj.put("status", "pending");
        returnObj.put("expires", DateTools.formatDateForACME(new Date()));
        returnObj.put("notBefore", DateTools.formatDateForACME(new Date()));
        returnObj.put("notAfter", DateTools.formatDateForACME(new Date()));
        returnObj.put("identifiers", respIdentifiersArr);
        returnObj.put("authorizations", respAuthorizationsArr);
        returnObj.put("finalize", provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");

        ctx.status(201);
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Content-Type", "application/jose+json");
        ctx.header("Location", provisioner.getApiURL() + "/acme/order/" + orderId);

        ctx.result(returnObj.toString());
    }


    /**
     * Checks if a given domain is allowed based on domain name restrictions defined in the ACME provisioner's configuration.
     *
     * @param domain The domain to be checked for permission.
     * @return True if the domain is allowed based on the configured restrictions or if restrictions are disabled;
     *         otherwise, false.
     */
    private boolean checkIfDomainIsAllowed(final String domain) {
        // Check if domain name restrictions are disabled
        if (!provisioner.getDomainNameRestriction().getEnabled()) {
            // Restriction is disabled, so any domain is allowed
            return true;
        }

        List<String> mustSuffix = provisioner.getDomainNameRestriction().getMustEndWith();

        for (String suffix : mustSuffix) {
            if (domain.endsWith(suffix)) {
                return true; // The domain ends with one of the permitted suffixes
            }
        }

        return false; // None of the suffixes match, and restrictions are enabled
    }

}
