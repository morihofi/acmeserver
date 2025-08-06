/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.certgine.acme.AcmeModuleInstance;
import de.morihofi.certgine.acme.security.NonceManager;
import de.morihofi.certgine.acme.servlets.handlerapi.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.objects.AcmeOrderResponse;
import de.morihofi.certgine.acme.security.SignatureCheck;
import de.morihofi.certgine.acme.servlets.handlerapi.objects.ACMERequestBody;

import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.AcmeHttpNonce;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.utils.datetime.TimeTools;
import de.morihofi.certgine.types.exception.exceptions.ACMEResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;


import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoint for retrieving information about an ACME order.
 * This class handles the request to fetch detailed information about a specific ACME order by its ID.
 */
@Slf4j
public class OrderInfoEndpoint extends AbstractAcmeEndpoint {

    private final Clock clock;

    /**
     * Constructs a new OrderInfoEndpoint instance with the specified provisioner and server instance.
     *
     * @param serverInstance The server instance for managing server configurations and operations.
     * @param clock          Clock used for time calculations.
     */
    public OrderInfoEndpoint(CertgineModuleInstance moduleInstance, Clock clock) {
        super(moduleInstance);
        this.clock = clock;
    }

    /**
     * Constructs a new OrderInfoEndpoint instance using the system UTC clock.
     *
     * @param serverInstance The server instance for managing server configurations and operations.
     */
    public OrderInfoEndpoint(CertgineModuleInstance moduleInstance) {
        this(moduleInstance, Clock.systemUTC());
    }

    /**
     * Handles the request to retrieve information about a specific ACME order.
     * This method sets the appropriate headers and response body with the details of the ACME order.
     *
     * @param ctx             The context of the HTTP request.
     * @param provisioner     The provisioner handling the ACME request.
     * @param gson            The Gson instance for JSON processing.
     * @param acmeRequestBody The body of the ACME request.
     */
    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) {
        String orderId = ctx.pathParam("orderId");

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", AcmeHttpNonce.createNonce(getModuleInstance().getModule().getServerInstance()));

        AcmeOrder order = AcmeOrder.getAcmeOrder(orderId, getModuleInstance().getModule().getServerInstance());
        List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> identifiers = order.getOrderIdentifiers();
        verifyIdentifiersPresent(orderId, identifiers);

        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, identifiers.getFirst().getOrder().getAccount(), gson, getModuleInstance().getModule().getServerInstance());
        ((AcmeModuleInstance) getModuleInstance()).getNonceManager().checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        boolean allVerified = true;
        List<AcmeOrderIdentifier> identifierList = new ArrayList<>();
        List<String> authorizationsList = new ArrayList<>();

        for (de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier identifier : identifiers) {
            if (identifier.getChallengeStatus() != AcmeStatus.VALID) {
                allVerified = false;
            }
            identifierList.add(new AcmeOrderIdentifier(identifier.getType(), identifier.getDataValue()));

            authorizationsList.add(provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/authz/" + identifier.getAuthorizationId());
        }

        AcmeOrderResponse response = new AcmeOrderResponse();
        response.setExpires(TimeTools.formatInstantForAcme(getOrderExpiration(order)));

        if (order.getCertificatePem() != null) {
            response.setStatus(AcmeStatus.VALID.getRfcName());
        } else {
            if (order.getCertificateCSR() != null) {
                // Processing means, that the certificate is being issued or in issue queue
                response.setStatus(AcmeStatus.PROCESSING.getRfcName());
            } else {
                // Ready means, that all authorizations are done. It is "ready" to process an CSR
                // Pending means, that some/all authorization are not verified at the moment
                response.setStatus(allVerified ? AcmeStatus.READY.getRfcName() : AcmeStatus.PENDING.getRfcName());
            }
        }

        response.setFinalize(provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/order/" + orderId + "/finalize");
        response.setCertificate(provisioner.getAcmeApiURL(getModuleInstance().getModule().getServerInstance()) + "/acme/order/" + orderId + "/cert");
        response.setIdentifiers(identifierList);
        response.setAuthorizations(authorizationsList);

        ctx.json(response);
    }

    /**
     * Returns the expiration timestamp for the provided order.
     *
     * @param order The ACME order.
     * @return The expiration {@link Instant} of the order.
     */
    Instant getOrderExpiration(@NonNull AcmeOrder order) {
        return order.getExpires();
    }

    /**
     * Ensures that an order contains at least one identifier.
     *
     * @param orderId    The ID of the order being validated.
     * @param identifiers The list of identifiers associated with the order.
     * @throws ACMEResourceNotFoundException if no identifiers are present.
     */
    void verifyIdentifiersPresent(@NonNull String orderId, @NonNull List<de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier> identifiers) {
        if (identifiers.isEmpty()) {
            log.error("Throwing API error: For the requested order {} was no identifier found", orderId);
            throw new ACMEResourceNotFoundException("For the requested order id was no identifier found");
        }
    }
}
