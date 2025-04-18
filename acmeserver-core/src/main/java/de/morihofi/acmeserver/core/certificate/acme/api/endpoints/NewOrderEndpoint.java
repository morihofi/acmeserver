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

package de.morihofi.acmeserver.core.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.core.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.core.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.objects.NewOrderRequestPayload;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.objects.NewOrderResponse;
import de.morihofi.acmeserver.core.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.core.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.core.database.AcmeStatus;
import de.morihofi.acmeserver.core.database.objects.ACMEAccount;
import de.morihofi.acmeserver.core.database.objects.ACMEOrder;
import de.morihofi.acmeserver.core.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.core.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.core.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.core.exception.exceptions.ACMERejectedIdentifierException;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import de.morihofi.acmeserver.core.tools.crypto.Crypto;
import de.morihofi.acmeserver.core.tools.dateAndTime.DateTools;
import de.morihofi.acmeserver.core.tools.regex.DomainAndIpValidation;
import io.javalin.http.Context;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.lang.invoke.MethodHandles;
import java.security.KeyStoreException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles the creation of new ACME orders.
 * <p>
 * URL: /acme/new-order
 */
@Slf4j
public class NewOrderEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger instance for logging NewOrderEndpoint activities.
     */


    /**
     * Constructs a NewOrderEndpoint with the given provisioner and server instance.
     *
     * @param serverInstance The server instance.
     */
    public NewOrderEndpoint(ServerInstance serverInstance) {
        super(serverInstance);
    }

    /**
     * Handles the ACME order request.
     * This method processes the request to create a new ACME order and stores it in the database.
     *
     * @param ctx             The context of the HTTP request.
     * @param provisioner     The provisioner instance.
     * @param gson            The Gson instance for JSON processing.
     * @param acmeRequestBody The parsed ACME request body.
     * @throws Exception If an error occurs while handling the request.
     */
    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        Provisioner p = getProvisioner(ctx);
        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        ACMEAccount account = ACMEAccount.getAccount(accountId, getServerInstance());
        // Check if account exists
        if (account == null) {
            log.error("Throwing API error: Account {} not found", accountId);
            throw new ACMEAccountNotFoundException("The account id was not found");
        }
        log.info("Account {} wants to create a new order", accountId);
        // Check signature and nonce
        performSignatureAndNonceCheck(ctx, accountId, acmeRequestBody);

        // Convert payload into object
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
            throw new ACMEInvalidContactException(
                    "This account doesn't have any E-Mail addresses. Please set at least one E-Mail address and try again.");
        }

        List<Identifier> respIdentifiers = new ArrayList<>();
        List<String> respAuthorizations = new ArrayList<>();

        List<ACMEOrderIdentifier> acmeOrderIdentifiersWithAuthorizationData = new ArrayList<>();

        // Unique certificate id per order
        String certificateId = Crypto.generateRandomId();

        for (ACMEOrderIdentifier identifier : acmeOrderIdentifiers) {
            // Unique value for each domain
            String authorizationId = Crypto.generateRandomId();

            // Only IP and DNS
            if (!(identifier.getType().equals("dns") || identifier.getType().equals("ip"))) {
                log.error("Throwing API error: Unknown or not allowed identifier type {} for value {}", identifier.getType(),
                        identifier.getDataValue());
                throw new ACMERejectedIdentifierException(
                        "Unknown identifier type \"" + identifier.getType() + "\" for value \"" + identifier.getDataValue() + "\"");
            }

            // Check DNS if type is DNS
            if (identifier.getType().equals("dns")) {
                if (!DomainAndIpValidation.isValidDomain(identifier.getDataValue(), provisioner.isWildcardAllowed())) {
                    throw new ACMERejectedIdentifierException(
                            "DNS-Identifier \"" + identifier.getDataValue() + "\" is invalid. (Wildcard allowed in provisioner: "
                                    + provisioner.isWildcardAllowed() + ")" +
                                    (DomainAndIpValidation.isIpAddress(identifier.getDataValue())
                                            ? " It looks like you put an IP Address into a DNS Identifier. Please use an "
                                            + "\"ip\"-identifier instead, if enabled in current provisioner."
                                            : ""));
                }

                if (!checkIfDomainIsAllowed(identifier.getDataValue(), p)) {
                    throw new ACMERejectedIdentifierException("Domain identifier \"" + identifier.getDataValue() + "\" is not allowed");
                }
            }

            // Check IP if type is IP
            if (identifier.getType().equals("ip")) {
                if (!p.isIpAllowed()) { // IP Address issuing is not allowed
                    throw new ACMERejectedIdentifierException("Issuing for IP Addresses has been disabled for this provisioner");
                }
                if (!DomainAndIpValidation.isIpAddress(identifier.getDataValue())) { // Not an IP Address
                    throw new ACMERejectedIdentifierException("IP-Identifier \"" + identifier.getDataValue() + "\" is invalid");
                }
            }

            identifier.setAuthorizationId(authorizationId);

            Identifier identifierObj = new Identifier();
            identifierObj.setType(identifier.getType());
            identifierObj.setValue(identifier.getDataValue());
            respIdentifiers.add(identifierObj);

            acmeOrderIdentifiersWithAuthorizationData.add(identifier);

            respAuthorizations.add(provisioner.getAcmeApiURL() + "/acme/authz/" + authorizationId);
        }

        ACMEOrder order;

        try (Session session = Objects.requireNonNull(getServerInstance().getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            Date startDate = new Date(); // Starts now
            Date endDate = calculateEndDate(newOrderRequestPayload, provisioner, startDate);

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

            log.info("Created new order {}", orderId);

            // Create order identifiers
            for (ACMEOrderIdentifier identifier : acmeOrderIdentifiersWithAuthorizationData) {
                identifier.setIdentifierId(Crypto.generateRandomId());
                identifier.setOrder(order);
                session.persist(identifier);

                log.info("Added identifier {} of type {} to order {}",
                        identifier.getDataValue(),
                        identifier.getType(),
                        orderId
                );
            }

            transaction.commit();
        }

        // FIXME Send E-Mail/Notification if order was created


        NewOrderResponse response = new NewOrderResponse();
        response.setStatus(AcmeStatus.PENDING.getRfcName());
        response.setExpires(DateTools.formatDateForACME(order.getExpires()));
        response.setNotBefore(DateTools.formatDateForACME(order.getNotBefore()));
        response.setNotAfter(DateTools.formatDateForACME(order.getNotAfter()));
        response.setIdentifiers(respIdentifiers);
        response.setAuthorizations(respAuthorizations);
        response.setFinalize(provisioner.getAcmeApiURL() + "/acme/order/" + orderId + "/finalize");

        ctx.status(201);
        ctx.header("Replay-Nonce", Crypto.createNonce(getServerInstance()));
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getAcmeApiURL() + "/acme/order/" + orderId);

        ctx.json(response);
    }

    /**
     * Checks if a given domain is allowed based on domain name restrictions defined in the ACME provisioner's configuration.
     *
     * @param domain The domain to be checked for permission.
     * @return True if the domain is allowed based on the configured restrictions or if restrictions are disabled;
     * otherwise, false.
     */
    private boolean checkIfDomainIsAllowed(final String domain, Provisioner p) {
        // Check if domain name restrictions are disabled
        if (!p.getDomainNameRestriction().isEnabled()) {
            // Restriction is disabled, so any domain is allowed
            return true;
        }

        List<String> mustSuffix = p.getDomainNameRestriction().getMustEndWith();

        for (String suffix : mustSuffix) {
            if (domain.endsWith(suffix)) {
                return true; // The domain ends with one of the permitted suffixes
            }
        }

        return false; // None of the suffixes match, and restrictions are enabled
    }

    /**
     * Calculates the notAfter property of the certificate. If the {@link NewOrderRequestPayload} provides a notAfter date,
     * and it does not exceed the notAfter of the intermediate CA certificate, it is returned.
     * Otherwise, the notAfter policy of the provisioner is returned.
     *
     * @param newOrderRequestPayload The payload of the new order request.
     * @param provisioner            The provisioner instance.
     * @param startDate              The start date of the order.
     * @return The calculated end date for the certificate.
     * @throws KeyStoreException if the intermediate CA certificate could not be loaded.
     */
    private Date calculateEndDate(NewOrderRequestPayload newOrderRequestPayload, Provisioner provisioner, Date startDate) throws KeyStoreException {
        Date endDateByOrder = newOrderRequestPayload.getNotAfter();

        Date endDateByCA = DateTools.makeDateForOutliveIntermediateCertificate(
                provisioner.getIntermediateCaCertificate().getNotAfter(),
                DateTools.addToDate(startDate,
                        provisioner.getGeneratedCertificateExpiration().getYears(),
                        provisioner.getGeneratedCertificateExpiration().getMonths(),
                        provisioner.getGeneratedCertificateExpiration().getDays()
                )
        );

        return endDateByOrder == null || endDateByOrder.after(endDateByCA)
                ? endDateByCA
                : endDateByOrder;
    }
}
