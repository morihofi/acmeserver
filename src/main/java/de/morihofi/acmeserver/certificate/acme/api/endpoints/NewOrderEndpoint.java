package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.NewOrderRequestPayload;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.NewOrderResponse;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMERejectedIdentifierException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import de.morihofi.acmeserver.tools.email.SendMail;
import de.morihofi.acmeserver.tools.regex.DomainAndIpValidation;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class NewOrderEndpoint extends AbstractAcmeEndpoint {


    /**
     * Logger
     */
    public final Logger log = LogManager.getLogger(getClass());


    public NewOrderEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        ACMEAccount account = Database.getAccount(accountId);
        //Check if account exists
        if (account == null) {
            log.error("Throwing API error: Account {} not found", accountId);
            throw new ACMEAccountNotFoundException("The account id was not found");
        }
        log.info("Account {} wants to create a new order", accountId);
        //Check signature and nonce
        performSignatureAndNonceCheck(ctx,accountId,acmeRequestBody);

        //Convert payload into object
        NewOrderRequestPayload newOrderRequestPayload = gson.fromJson(acmeRequestBody.getDecodedPayload(), NewOrderRequestPayload.class);

        ArrayList<ACMEOrderIdentifier> acmeOrderIdentifiers = new ArrayList<>();

        for (Identifier identifier : newOrderRequestPayload.getIdentifiers()) {
            String type = identifier.getType();
            String value = identifier.getValue();

            acmeOrderIdentifiers.add(new ACMEOrderIdentifier(type, value));
        }

        // Create order in Database
        String orderId = UUID.randomUUID().toString();

        if (account.getEmails().isEmpty()) {
            throw new ACMEInvalidContactException("This account doesn't have any E-Mail addresses. Please set at least one E-Mail address and try again.");
        }


        List<Identifier> respIdentifiers = new ArrayList<>();
        List<String> respAuthorizations = new ArrayList<>();

        List<ACMEOrderIdentifier> acmeOrderIdentifiersWithAuthorizationData = new ArrayList<>();



        // Unique certificate id per order
        String certificateId = Crypto.generateRandomId();


        for (ACMEOrderIdentifier identifier : acmeOrderIdentifiers) {
            // Unique value for each domain
            String authorizationId = Crypto.generateRandomId();

            //Only IP and DNS
            if (!(identifier.getType().equals("dns") || identifier.getType().equals("ip"))) {
                log.error("Throwing API error: Unknown or not allowed identifier type {} for value {}", identifier.getType(), identifier.getDataValue());
                throw new ACMERejectedIdentifierException("Unknown identifier type \"" + identifier.getType() + "\" for value \"" + identifier.getDataValue() + "\"");
            }

            //Check DNS if type is DNS
            if(identifier.getType().equals("dns")){
                if (!DomainAndIpValidation.isValidDomain(identifier.getDataValue(), provisioner.isWildcardAllowed())) {
                    throw new ACMERejectedIdentifierException("Identifier \"" + identifier.getDataValue() + "\" is invalid (Wildcard allowed: " + provisioner.isWildcardAllowed() + ")");

                }

                if (!checkIfDomainIsAllowed(identifier.getDataValue())) {
                    throw new ACMERejectedIdentifierException("Domain identifier \"" + identifier.getDataValue() + "\" is not allowed");
                }
            }

            //Check IP if type is IP
            if(identifier.getType().equals("ip")){
                if (!DomainAndIpValidation.isIpAddress(identifier.getDataValue())) {
                    throw new ACMERejectedIdentifierException("Identifier IP \"" + identifier.getDataValue() + "\" is invalid");
                }
            }

            identifier.setAuthorizationId(authorizationId);

            Identifier identifierObj = new Identifier();
            identifierObj.setType(identifier.getType());
            identifierObj.setValue(identifier.getDataValue());
            respIdentifiers.add(identifierObj);

            acmeOrderIdentifiersWithAuthorizationData.add(identifier);

            respAuthorizations.add(provisioner.getApiURL() + "/acme/authz/" + authorizationId);


        }

        // Add authorizations to Database
        Database.createOrder(account, orderId, acmeOrderIdentifiersWithAuthorizationData, certificateId);

        //Send E-Mail if order was created
        try {
            SendMail.sendMail(account.getEmails().get(0), "New ACME order created", "Hey there, <br> a new ACME order (" + orderId + ") for <i>" + acmeOrderIdentifiers.get(0).getDataValue() + "</i> was created.");
        } catch (Exception ex) {
            log.error("Unable to send email", ex);
        }


        //TODO: Set better Date/Time
        NewOrderResponse response = new NewOrderResponse();
        response.setStatus(AcmeStatus.PENDING.getRfcName());
        response.setExpires(DateTools.formatDateForACME(new Date()));
        response.setNotBefore(DateTools.formatDateForACME(new Date()));
        response.setNotAfter(DateTools.formatDateForACME(new Date()));
        response.setIdentifiers(respIdentifiers);
        response.setAuthorizations(respAuthorizations);
        response.setFinalize(provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");


        ctx.status(201);
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getApiURL() + "/acme/order/" + orderId);

        ctx.result(gson.toJson(response));
    }


    /**
     * Checks if a given domain is allowed based on domain name restrictions defined in the ACME provisioner's configuration.
     *
     * @param domain The domain to be checked for permission.
     * @return True if the domain is allowed based on the configured restrictions or if restrictions are disabled;
     * otherwise, false.
     */
    private boolean checkIfDomainIsAllowed(final String domain) {
        // Check if domain name restrictions are disabled
        if (!getProvisioner().getDomainNameRestriction().getEnabled()) {
            // Restriction is disabled, so any domain is allowed
            return true;
        }

        List<String> mustSuffix = getProvisioner().getDomainNameRestriction().getMustEndWith();

        for (String suffix : mustSuffix) {
            if (domain.endsWith(suffix)) {
                return true; // The domain ends with one of the permitted suffixes
            }
        }

        return false; // None of the suffixes match, and restrictions are enabled
    }

}
