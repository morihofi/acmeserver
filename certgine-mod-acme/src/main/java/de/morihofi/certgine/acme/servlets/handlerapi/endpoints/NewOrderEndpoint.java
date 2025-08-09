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
import de.morihofi.certgine.acme.types.events.AcmePendingOrderEvent;
import de.morihofi.certgine.acme.types.events.NewAcmeOrderEvent;
import de.morihofi.certgine.acme.util.AcmeTimeHelper;
import de.morihofi.certgine.cryptography.randomness.RandomGenerator;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.certgine.types.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.certgine.types.exception.exceptions.ACMERejectedIdentifierException;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.types.intf.IServerInstance;
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
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson,
                              @NonNull ACMERequestBody acmeRequestBody) throws Exception {
        IServerInstance serverInstance = getModuleInstance().getModule().getServerInstance();
        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        AcmeAccount account = AcmeAccount.getAccount(accountId, serverInstance);

        if (account == null) {
            log.error("Throwing API error: Account {} not found", accountId);
            throw new ACMEAccountNotFoundException("The account id was not found");
        }

        log.info("Account {} wants to create a new order", accountId);
        performSignatureAndNonceCheck(ctx, accountId, acmeRequestBody);

        NewOrderRequestPayload newOrderRequestPayload =
                gson.fromJson(acmeRequestBody.getDecodedPayload(), NewOrderRequestPayload.class);

        if (account.getEmails().isEmpty()) {
            throw new ACMEInvalidContactException(
                    "This account doesn't have any E-Mail addresses. Please set at least one E-Mail address and try again.");
        }

        String orderId = UUID.randomUUID().toString();
        IdentifierProcessingResult identifierResult =
                processIdentifiers(newOrderRequestPayload.getIdentifiers(), provisioner, serverInstance);

        String certificateId = HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId());
        Instant startInstant = clock.instant();
        Instant endInstant = calculateEndInstant(newOrderRequestPayload, provisioner, startInstant);

        AcmeOrder order = persistOrder(orderId, certificateId, account,
                identifierResult.getEntityIdentifiers(), startInstant, endInstant, serverInstance);

        serverInstance.getEventBus().publish(new AcmePendingOrderEvent(account, order));

        NewOrderResponse response = buildNewOrderResponse(provisioner, serverInstance, order,
                identifierResult.getResponseIdentifiers(), identifierResult.getAuthorizations());

        ctx.status(HttpURLConnection.HTTP_CREATED);
        ctx.header("Replay-Nonce", AcmeHttpNonce.createNonce(serverInstance));
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getAcmeApiURL(serverInstance) + "/acme/order/" + orderId);

        ctx.json(response);
    }
    
    /**
     * Validates the provided identifiers and prepares authorization data for persistence and response.
     *
     * @param requestIdentifiers identifiers received in the order request
     * @param provisioner       current provisioner
     * @param serverInstance    server instance for URL construction
     * @return processed identifiers containing entity and response information
     * @throws ACMERejectedIdentifierException if any identifier is not acceptable
     */
    private IdentifierProcessingResult processIdentifiers(List<AcmeOrderIdentifier> requestIdentifiers,
                                                          AcmeProvisioner provisioner,
                                                          IServerInstance serverInstance) throws ACMERejectedIdentifierException {
        List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> entities = new ArrayList<>();
        List<AcmeOrderIdentifier> responseIdentifiers = new ArrayList<>();
        List<String> authorizations = new ArrayList<>();

        for (AcmeOrderIdentifier identifier : requestIdentifiers) {
            de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier entity =
                    new de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier(identifier.getType(), identifier.getValue());
            String authorizationId = HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId());

            if (!(entity.getType().equals("dns") || entity.getType().equals("ip"))) {
                log.error("Throwing API error: Unknown or not allowed identifier type {} for value {}", entity.getType(),
                        entity.getDataValue());
                throw new ACMERejectedIdentifierException(
                        "Unknown identifier type \"" + entity.getType() + "\" for value \"" + entity.getDataValue() + "\"");
            }

            if (entity.getType().equals("dns")) {
                if (!DomainValidator.isValidDomain(entity.getDataValue(), provisioner.isWildcardAllowed())) {
                    throw new ACMERejectedIdentifierException(
                            "DNS-Identifier \"" + entity.getDataValue() + "\" is invalid. (Wildcard allowed in provisioner:" +
                                    provisioner.isWildcardAllowed() + ")" +
                                    (IpValidator.isIpAddress(entity.getDataValue())
                                            ? " It looks like you put an IP Address into a DNS Identifier. Please use an " +
                                            "\"ip\"-identifier instead, if enabled in current provisioner."
                                            : ""));
                }
                if (!checkIfDomainIsAllowed(entity.getDataValue(), provisioner)) {
                    throw new ACMERejectedIdentifierException("Domain identifier \"" + entity.getDataValue() + "\" is not allowed");
                }
            }

            if (entity.getType().equals("ip")) {
                if (!provisioner.isIpAllowed()) {
                    throw new ACMERejectedIdentifierException("Issuing for IP Addresses has been disabled for this provisioner");
                }
                if (!IpValidator.isIpAddress(entity.getDataValue())) {
                    throw new ACMERejectedIdentifierException("IP-Identifier \"" + entity.getDataValue() + "\" is invalid");
                }
            }

            entity.setAuthorizationId(authorizationId);

            AcmeOrderIdentifier respId = new AcmeOrderIdentifier();
            respId.setType(entity.getType());
            respId.setValue(entity.getDataValue());

            entities.add(entity);
            responseIdentifiers.add(respId);
            authorizations.add(provisioner.getAcmeApiURL(serverInstance) + "/acme/authz/" + authorizationId);
        }

        return new IdentifierProcessingResult(entities, responseIdentifiers, authorizations);
    }

    /**
     * Persists the ACME order and its identifiers in the database.
     *
     * @param orderId      unique order identifier
     * @param certificateId unique certificate id
     * @param account      owning account
     * @param identifiers  prepared identifiers to persist
     * @param startInstant start time of the order
     * @param endInstant   expiration time of the order
     * @param serverInstance server instance providing database access
     * @return persisted {@link AcmeOrder}
     */
    private AcmeOrder persistOrder(String orderId, String certificateId, AcmeAccount account,
                                   List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> identifiers,
                                   Instant startInstant, Instant endInstant, IServerInstance serverInstance) {
        AcmeOrder order;
        try (Session session = serverInstance.getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

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

            for (de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier identifier : identifiers) {
                identifier.setIdentifierId(HexConverter.bigIntegerAsHexString(RandomGenerator.generateRandomId()));
                identifier.setOrder(order);
                session.persist(identifier);

                log.info("Added identifier {} of type {} to order {} (authorizationId: {})",
                        identifier.getDataValue(),
                        identifier.getType(),
                        orderId,
                        identifier.getAuthorizationId());
            }

            transaction.commit();
            serverInstance.getEventBus().publish(new NewAcmeOrderEvent(order));
        }
        return order;
    }

    /**
     * Builds the response object for a newly created order.
     *
     * @param provisioner       current provisioner
     * @param serverInstance    server instance for URL construction
     * @param order             persisted order
     * @param respIdentifiers   identifiers to include in response
     * @param respAuthorizations authorization URLs
     * @return prepared {@link NewOrderResponse}
     */
    private NewOrderResponse buildNewOrderResponse(AcmeProvisioner provisioner, IServerInstance serverInstance,
                                                   AcmeOrder order,
                                                   List<AcmeOrderIdentifier> respIdentifiers,
                                                   List<String> respAuthorizations) {
        NewOrderResponse response = new NewOrderResponse();
        response.setStatus(AcmeStatus.PENDING.getRfcName());
        response.setExpires(AcmeTimeHelper.formatInstantForAcme(order.getExpires()));
        response.setNotBefore(AcmeTimeHelper.formatInstantForAcme(order.getNotBefore()));
        response.setNotAfter(AcmeTimeHelper.formatInstantForAcme(order.getNotAfter()));
        response.setIdentifiers(respIdentifiers);
        response.setAuthorizations(respAuthorizations);
        response.setFinalize(provisioner.getAcmeApiURL(serverInstance) + "/acme/order/" + order.getOrderId() + "/finalize");
        return response;
    }

    static class IdentifierProcessingResult {
        private final List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> entityIdentifiers;
        private final List<AcmeOrderIdentifier> responseIdentifiers;
        private final List<String> authorizations;

        IdentifierProcessingResult(
                List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> entityIdentifiers,
                List<AcmeOrderIdentifier> responseIdentifiers,
                List<String> authorizations) {
            this.entityIdentifiers = entityIdentifiers;
            this.responseIdentifiers = responseIdentifiers;
            this.authorizations = authorizations;
        }

        List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> getEntityIdentifiers() {
            return entityIdentifiers;
        }

        List<AcmeOrderIdentifier> getResponseIdentifiers() {
            return responseIdentifiers;
        }

        List<String> getAuthorizations() {
            return authorizations;
        }
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
