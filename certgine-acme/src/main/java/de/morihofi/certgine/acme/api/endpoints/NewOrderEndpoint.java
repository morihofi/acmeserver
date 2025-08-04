/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints;

import com.google.gson.Gson;

import de.morihofi.certgine.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.api.acme.dns.Identifier;
import de.morihofi.certgine.acme.api.endpoints.objects.NewOrderRequestPayload;
import de.morihofi.certgine.acme.api.endpoints.objects.NewOrderResponse;
import de.morihofi.certgine.acme.security.SignatureCheck;
import de.morihofi.certgine.acme.api.objects.ACMERequestBody;
import de.morihofi.certgine.types.database.entities.acme.*;
import de.morihofi.certgine.types.database.entities.acme.enums.AcmeStatus;
import de.morihofi.certgine.types.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.certgine.types.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.certgine.types.exception.exceptions.ACMERejectedIdentifierException;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.conversion.HexConverter;
import de.morihofi.certgine.utils.datetime.TimeTools;
import de.morihofi.certgine.utils.regex.DomainValidator;
import de.morihofi.certgine.utils.regex.IpValidator;
import de.morihofi.certgine.types.events.NewAcmeOrderEvent;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.net.HttpURLConnection;
import java.security.KeyStoreException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the creation of new ACME orders.
 * <p>
 * URL: /acme/new-order
 */
@Slf4j
public class NewOrderEndpoint extends AbstractAcmeEndpoint {

    private final Clock clock;

    /**
     * Constructs a NewOrderEndpoint with the given provisioner and server instance.
     *
     * @param serverInstance The server instance.
     * @param clock         Clock used for time calculations.
     */
    public NewOrderEndpoint(IServerInstance serverInstance, Clock clock) {
        super(serverInstance);
        this.clock = clock;
    }

    /**
     * Constructs a NewOrderEndpoint using the system UTC clock.
     *
     * @param serverInstance The server instance.
     */
    public NewOrderEndpoint(IServerInstance serverInstance) {
        this(serverInstance, Clock.systemUTC());
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
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        AcmeAccount account = AcmeAccount.getAccount(accountId, getServerInstance());

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

        List<AcmeOrderIdentifier> AcmeOrderIdentifiers = new ArrayList<>();

        for (Identifier identifier : newOrderRequestPayload.getIdentifiers()) {
            String type = identifier.getType();
            String value = identifier.getValue();

            AcmeOrderIdentifiers.add(new AcmeOrderIdentifier(type, value));
        }

        // Create order in Database
        String orderId = UUID.randomUUID().toString();

        if (account.getEmails().isEmpty()) {
            throw new ACMEInvalidContactException(
                    "This account doesn't have any E-Mail addresses. Please set at least one E-Mail address and try again.");
        }

        List<Identifier> respIdentifiers = new ArrayList<>();
        List<String> respAuthorizations = new ArrayList<>();

        List<AcmeOrderIdentifier> AcmeOrderIdentifiersWithAuthorizationData = new ArrayList<>();

        // Unique certificate id per order
        String certificateId = HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId());

        for (AcmeOrderIdentifier identifier : AcmeOrderIdentifiers) {
            // Unique value for each domain
            String authorizationId = HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId());

            // Only IP and DNS
            if (!(identifier.getType().equals("dns") || identifier.getType().equals("ip"))) {
                log.error("Throwing API error: Unknown or not allowed identifier type {} for value {}", identifier.getType(),
                        identifier.getDataValue());
                throw new ACMERejectedIdentifierException(
                        "Unknown identifier type \"" + identifier.getType() + "\" for value \"" + identifier.getDataValue() + "\"");
            }

            // Check DNS if type is DNS
            if (identifier.getType().equals("dns")) {
                if (!DomainValidator.isValidDomain(identifier.getDataValue(), provisioner.isWildcardAllowed())) {
                    throw new ACMERejectedIdentifierException(
                            "DNS-Identifier \"" + identifier.getDataValue() + "\" is invalid. (Wildcard allowed in provisioner: "
                                    + provisioner.isWildcardAllowed() + ")" +
                                    (IpValidator.isIpAddress(identifier.getDataValue())
                                            ? " It looks like you put an IP Address into a DNS Identifier. Please use an "
                                            + "\"ip\"-identifier instead, if enabled in current provisioner."
                                            : ""));
                }

                if (!checkIfDomainIsAllowed(identifier.getDataValue(), provisioner)) {
                    throw new ACMERejectedIdentifierException("Domain identifier \"" + identifier.getDataValue() + "\" is not allowed");
                }
            }

            // Check IP if type is IP
            if (identifier.getType().equals("ip")) {
                if (!provisioner.isIpAllowed()) { // IP Address issuing is not allowed
                    throw new ACMERejectedIdentifierException("Issuing for IP Addresses has been disabled for this provisioner");
                }
                if (!IpValidator.isIpAddress(identifier.getDataValue())) { // Not an IP Address
                    throw new ACMERejectedIdentifierException("IP-Identifier \"" + identifier.getDataValue() + "\" is invalid");
                }
            }

            identifier.setAuthorizationId(authorizationId);

            Identifier identifierObj = new Identifier();
            identifierObj.setType(identifier.getType());
            identifierObj.setValue(identifier.getDataValue());
            respIdentifiers.add(identifierObj);

            AcmeOrderIdentifiersWithAuthorizationData.add(identifier);

            respAuthorizations.add(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/authz/" + authorizationId);
        }

        AcmeOrder order;

        try (Session session = getServerInstance().getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            Instant startInstant = clock.instant(); // Starts now
            Instant endInstant = calculateEndInstant(newOrderRequestPayload, provisioner, startInstant);

            // Create order
            order = new AcmeOrder();
            order.setOrderId(orderId);
            order.setAccount(account);
            order.setCreated(java.sql.Timestamp.from(startInstant));
            order.setExpires(java.sql.Timestamp.from(endInstant));
            order.setNotBefore(java.sql.Timestamp.from(startInstant));
            order.setNotAfter(java.sql.Timestamp.from(endInstant));
            order.setCertificateId(certificateId);
            session.persist(order);

            log.info("Created new order {}", orderId);

            // Create order identifiers
            for (AcmeOrderIdentifier identifier : AcmeOrderIdentifiersWithAuthorizationData) {
                identifier.setIdentifierId(HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId()));
                identifier.setOrder(order);
                identifier.setAuthorizationId(identifier.getAuthorizationId());

                session.persist(identifier);

                log.info("Added identifier {} of type {} to order {} (authorizationId: {})",
                        identifier.getDataValue(),
                        identifier.getType(),
                        orderId,
                        identifier.getAuthorizationId()
                );
            }

            transaction.commit();
            getServerInstance().getEventBus().publish(new NewAcmeOrderEvent(order));
        }

        // FIXME Send E-Mail/Notification if order was created


        NewOrderResponse response = new NewOrderResponse();
        response.setStatus(AcmeStatus.PENDING.getRfcName());
        response.setExpires(TimeTools.formatInstantForAcme(order.getExpires().toInstant()));
        response.setNotBefore(TimeTools.formatInstantForAcme(order.getNotBefore().toInstant()));
        response.setNotAfter(TimeTools.formatInstantForAcme(order.getNotAfter().toInstant()));
        response.setIdentifiers(respIdentifiers);
        response.setAuthorizations(respAuthorizations);
        response.setFinalize(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/order/" + orderId + "/finalize");

        ctx.status(HttpURLConnection.HTTP_CREATED);
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getAcmeApiURL(getServerInstance()) + "/acme/order/" + orderId);

        ctx.json(response);
    }

    /**
     * Checks if a given domain is allowed based on domain name restrictions defined in the ACME provisioner's configuration.
     *
     * @param domain The domain to be checked for permission.
     * @return True if the domain is allowed based on the configured restrictions or if restrictions are disabled;
     * otherwise, false.
     */
    private boolean checkIfDomainIsAllowed(@NonNull final String domain, @NonNull AcmeProvisioner p) {
        // Check if domain name restrictions are disabled
        if (!p.getAcmeProvisionerDomainNameRestriction().isEnabled()) {
            // Restriction is disabled, so any domain is allowed
            return true;
        }

        List<String> mustSuffix = p.getAcmeProvisionerDomainNameRestriction().getMustEndWith();

        for (String suffix : mustSuffix) {
            if (domain.endsWith(suffix)) {
                return true; // The domain ends with one of the permitted suffixes
            }
        }

        return false; // None of the suffixes match, and restrictions are enabled
    }

    /**
     * Calculates the {@code notAfter} value for the certificate. If the {@link NewOrderRequestPayload}
     * provides a {@code notAfter} instant that does not exceed the intermediate CA certificate's
     * expiration, that value is used. Otherwise, the provisioner's validity policy is applied.
     *
     * @param newOrderRequestPayload the payload of the new order request
     * @param provisioner            the provisioner instance
     * @param start                  the start instant of the order
     * @return the calculated end instant for the certificate
     * @throws KeyStoreException if the intermediate CA certificate could not be loaded
     */
    private Instant calculateEndInstant(@NonNull NewOrderRequestPayload newOrderRequestPayload,
                                        @NonNull AcmeProvisioner provisioner,
                                        @NonNull Instant start) throws KeyStoreException {
        Instant endByOrder = newOrderRequestPayload.getNotAfter();

        Instant endByCa = TimeTools.makeInstantForOutliveIntermediateCertificate(
                getServerInstance().getCryptoStoreManager()
                        .getIntermediateCertificate(provisioner.getInternalUuid())
                        .getNotAfter().toInstant(),
                TimeTools.addToInstant(start,
                        provisioner.getIssuedCertificateExpiration().getYears(),
                        provisioner.getIssuedCertificateExpiration().getMonths(),
                        provisioner.getIssuedCertificateExpiration().getDays())
        );

        return endByOrder == null || endByOrder.isAfter(endByCa) ? endByCa : endByOrder;
    }
}
