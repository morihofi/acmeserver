/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints;

import com.google.gson.Gson;
import de.morihofi.certgine.acme.security.SignatureCheck;
import de.morihofi.certgine.acme.servlets.handlerapi.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.objects.NewOrderRequestPayload;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.objects.NewOrderResponse;
import de.morihofi.certgine.acme.servlets.handlerapi.objects.ACMERequestBody;
import de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier;
import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.acme.types.entities.AcmeHttpNonce;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.acme.types.events.NewAcmeOrderEvent;
import de.morihofi.certgine.acme.util.AcmeTimeHelper;
import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.certgine.types.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.certgine.types.exception.exceptions.ACMERejectedIdentifierException;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.utils.conversion.HexConverter;
import de.morihofi.certgine.utils.datetime.TimeTools;
import de.morihofi.certgine.utils.regex.DomainValidator;
import de.morihofi.certgine.utils.regex.IpValidator;
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
     * @param clock          Clock used for time calculations.
     */
    public NewOrderEndpoint(CertgineModuleInstance moduleInstance, Clock clock) {
        super(moduleInstance);
        this.clock = clock;
    }

    /**
     * Constructs a NewOrderEndpoint using the system UTC clock.
     *
     * @param serverInstance The server instance.
     */
    public NewOrderEndpoint(CertgineModuleInstance moduleInstance) {
        this(moduleInstance, Clock.systemUTC());
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
        AcmeAccount account = AcmeAccount.getAccount(accountId, getModuleInstance().getModule().getServerInstance());

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

        List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> AcmeOrderIdentifiers = new ArrayList<>();

        for (AcmeOrderIdentifier identifier : newOrderRequestPayload.getIdentifiers()) {
            String type = identifier.getType();
            String value = identifier.getValue();

            AcmeOrderIdentifiers.add(new de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier(type, value));
        }

        // Create order in Database
        String orderId = UUID.randomUUID().toString();

        if (account.getEmails().isEmpty()) {
            throw new ACMEInvalidContactException(
                    "This account doesn't have any E-Mail addresses. Please set at least one E-Mail address and try again.");
        }

        List<de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier> respIdentifiers = new ArrayList<>();
        List<String> respAuthorizations = new ArrayList<>();

        List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> AcmeOrderIdentifiersWithAuthorizationData = new ArrayList<>();

        // Unique certificate id per order
        String certificateId = HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId());

        for (de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier identifier : AcmeOrderIdentifiers) {
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

            AcmeOrderIdentifier identifierObj = new AcmeOrderIdentifier();
            identifierObj.setType(identifier.getType());
            identifierObj.setValue(identifier.getDataValue());
            respIdentifiers.add(identifierObj);

            AcmeOrderIdentifiersWithAuthorizationData.add(identifier);

            respAuthorizations.add(provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/authz/" + authorizationId);
        }

        AcmeOrder order;

        try (Session session = getModuleInstance().getModule().getServerInstance().getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            Instant startInstant = clock.instant(); // Starts now
            Instant endInstant = calculateEndInstant(newOrderRequestPayload, provisioner, startInstant);

            // Create order
            order = new AcmeOrder();
            order.setOrderId(orderId);
            order.setAccount(account);
            order.setCreated(startInstant);
            order.setExpires(endInstant);
            order.setNotBefore(startInstant);
            order.setNotAfter(endInstant);
            order.setCertificateId(certificateId);
            session.persist(order);

            log.info("Created new order {}", orderId);

            // Create order identifiers
            for (de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier identifier : AcmeOrderIdentifiersWithAuthorizationData) {
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
            getModuleInstance().getModule().getServerInstance().getEventBus().publish(new NewAcmeOrderEvent(order));
        }

        // FIXME Send E-Mail/Notification if order was created


        NewOrderResponse response = new NewOrderResponse();
        response.setStatus(AcmeStatus.PENDING.getRfcName());
        response.setExpires(AcmeTimeHelper.formatInstantForAcme(order.getExpires()));
        response.setNotBefore(AcmeTimeHelper.formatInstantForAcme(order.getNotBefore()));
        response.setNotAfter(AcmeTimeHelper.formatInstantForAcme(order.getNotAfter()));
        response.setIdentifiers(respIdentifiers);
        response.setAuthorizations(respAuthorizations);
        response.setFinalize(provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/order/" + orderId + "/finalize");

        ctx.status(HttpURLConnection.HTTP_CREATED);
        ctx.header("Replay-Nonce", AcmeHttpNonce.createNonce(getModuleInstance().getModule().getServerInstance()));
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/order/" + orderId);

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
                getModuleInstance()
                        .getModule()
                        .getServerInstance()
                        .getCryptoStoreManager()
                        .getIntermediateCertificate(provisioner.getInternalUuid())
                        .getNotAfter()
                        .toInstant(),
                TimeTools.addToInstant(start,
                        provisioner.getIssuedCertificateExpiration().getYears(),
                        provisioner.getIssuedCertificateExpiration().getMonths(),
                        provisioner.getIssuedCertificateExpiration().getDays())
        );

        return endByOrder == null || endByOrder.isAfter(endByCa) ? endByCa : endByOrder;
    }
}
