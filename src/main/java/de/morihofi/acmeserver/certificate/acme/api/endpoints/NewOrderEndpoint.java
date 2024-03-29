package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.NewOrderRequestPayload;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.NewOrderResponse;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
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
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.*;

public class NewOrderEndpoint extends AbstractAcmeEndpoint {


    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());


    public NewOrderEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        ACMEAccount account = ACMEAccount.getAccount(accountId);
        //Check if account exists
        if (account == null) {
            LOG.error("Throwing API error: Account {} not found", accountId);
            throw new ACMEAccountNotFoundException("The account id was not found");
        }
        LOG.info("Account {} wants to create a new order", accountId);
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
                LOG.error("Throwing API error: Unknown or not allowed identifier type {} for value {}", identifier.getType(), identifier.getDataValue());
                throw new ACMERejectedIdentifierException("Unknown identifier type \"" + identifier.getType() + "\" for value \"" + identifier.getDataValue() + "\"");
            }

            //Check DNS if type is DNS
            if(identifier.getType().equals("dns")){
                if (!DomainAndIpValidation.isValidDomain(identifier.getDataValue(), provisioner.isWildcardAllowed())) {
                    throw new ACMERejectedIdentifierException("DNS-Identifier \"" + identifier.getDataValue() + "\" is invalid. (Wildcard allowed in provisioner: " + provisioner.isWildcardAllowed() + ")" +
                            (DomainAndIpValidation.isIpAddress(identifier.getDataValue()) ? " It looks like you put an IP Address into a DNS Identifier. Please use am \"ip\"-identifier instead, if enabled in current provisioner." : ""));

                }

                if (!checkIfDomainIsAllowed(identifier.getDataValue())) {
                    throw new ACMERejectedIdentifierException("Domain identifier \"" + identifier.getDataValue() + "\" is not allowed");
                }
            }

            //Check IP if type is IP
            if(identifier.getType().equals("ip")){
                if(!getProvisioner().isIpAllowed()){ //IP Address issuing is not allowed
                    throw new ACMERejectedIdentifierException("Issuing for IP Addresses has been disabled for this provisioner");
                }
                if (!DomainAndIpValidation.isIpAddress(identifier.getDataValue())) { //Not an IP Address
                    throw new ACMERejectedIdentifierException("IP-Identifier \"" + identifier.getDataValue() + "\" is invalid");
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


        ACMEOrder order;

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();



            Date startDate = new Date(); // Starts now
            Date endDate = DateTools.makeDateForOutliveIntermediateCertificate(
                    provisioner.getIntermediateCaCertificate().getNotAfter(),
                    DateTools.addToDate(startDate,
                        provisioner.getConfig().getIntermediate().getExpiration().getYears(),
                        provisioner.getConfig().getIntermediate().getExpiration().getMonths(),
                        provisioner.getConfig().getIntermediate().getExpiration().getDays()
                    )
            );


            // Create order
            order = new ACMEOrder();
            order.setOrderId(orderId);
            order.setAccount(account);
            order.setCreated(Timestamp.from(startDate.toInstant()));
            order.setExpires(Timestamp.from(endDate.toInstant()));
            order.setNotBefore(Timestamp.from(startDate.toInstant()));
            order.setNotAfter(Timestamp.from(endDate.toInstant()));
            order.setCertificateId(certificateId);
            session.persist(order);

            LOG.info("Created new order {}", orderId);

            // Create order identifiers
            for (ACMEOrderIdentifier identifier : acmeOrderIdentifiersWithAuthorizationData) {
                identifier.setIdentifierId(Crypto.generateRandomId());
                identifier.setOrder(order);
                session.persist(identifier);

                LOG.info("Added identifier {} of type {} to order {}",
                        identifier.getDataValue(),
                        identifier.getType(),
                        orderId
                );
            }

            transaction.commit();
        }


        //Send E-Mail if order was created
        try {
            SendMail.sendMail(account.getEmails().get(0), "New ACME order created", "Hey there, <br> a new ACME order (" + orderId + ") for <i>" + acmeOrderIdentifiers.get(0).getDataValue() + "</i> was created.");
        } catch (Exception ex) {
            LOG.error("Unable to send email", ex);
        }

        NewOrderResponse response = new NewOrderResponse();
        response.setStatus(AcmeStatus.PENDING.getRfcName());
        response.setExpires(DateTools.formatDateForACME(order.getExpires()));
        response.setNotBefore(DateTools.formatDateForACME(order.getNotBefore()));
        response.setNotAfter(DateTools.formatDateForACME(order.getNotAfter()));
        response.setIdentifiers(respIdentifiers);
        response.setAuthorizations(respAuthorizations);
        response.setFinalize(provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");


        ctx.status(201);
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getApiURL() + "/acme/order/" + orderId);

        ctx.json(response);
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
