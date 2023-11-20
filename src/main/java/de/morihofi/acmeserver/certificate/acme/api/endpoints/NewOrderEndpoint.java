package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMERejectedIdentifierException;
import de.morihofi.acmeserver.tools.Crypto;
import de.morihofi.acmeserver.tools.DateTools;
import de.morihofi.acmeserver.tools.DomainValidation;
import de.morihofi.acmeserver.tools.SendMail;
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
    /*
    ChatGPT ACME Guide approved class
    */

    private Provisioner provisioner;
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

            if(!DomainValidation.isValidDomain(identifier.getDataValue())){
                throw new ACMERejectedIdentifierException("Identifier \"" + identifier.getDataValue() + "\" is invalid");

            }

            if(!checkIfDomainIsAllowed(identifier.getDataValue())){
                throw new ACMERejectedIdentifierException("Domain identifier \"" + identifier.getDataValue() + "\" is not allowed");
            }

            JSONObject identifierObj = new JSONObject();
            identifierObj.put("type", identifier.getType());
            identifierObj.put("value", identifier.getDataValue());


            respIdentifiersArr.put(identifierObj);

            // Unique value for each domain
            String authorizationId = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + DateTools.formatDateForACME(new Date()));
            // Random authorization token
            String authorizationToken = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + System.nanoTime() + "--token");
            // Unique challenge id
            String challengeId = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + (System.nanoTime() / 100 * 1.557) + "--challenge");
            // Unique certificate id
            String certificateId = Crypto.hashStringSHA256(identifier.getType() + "." + identifier.getType() + "." + (System.nanoTime() / 100 * 5.579) + "--cert");


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


    private boolean checkIfDomainIsAllowed(String domain) {

        if(!provisioner.getDomainNameRestriction().getEnabled()){
            //Restriction is disabled, so anything is allowed
            return true;
        }

        List<String> mustSuffix = provisioner.getDomainNameRestriction().getMustEndWith();

        for (String suffix : mustSuffix) {
            if (domain.endsWith(suffix)) {
                return true; // The domain ends with one of the permitted suffixes
            }
        }

        return false; // None of the suffixes fit
    }
}
