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

package de.morihofi.acmeserver.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.acmeserver.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.acme.api.endpoints.order.objects.AcmeOrderResponse;
import de.morihofi.acmeserver.acme.security.SignatureCheck;
import de.morihofi.acmeserver.acme.api.objects.ACMERequestBody;

import de.morihofi.acmeserver.types.database.entities.acme.enums.AcmeStatus;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrderIdentifier;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.acme.HttpNonces;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.utils.datetime.DateTools;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Endpoint for retrieving information about an ACME order.
 * This class handles the request to fetch detailed information about a specific ACME order by its ID.
 */
@Slf4j
public class OrderInfoEndpoint extends AbstractAcmeEndpoint {


    /**
     * Constructs a new OrderInfoEndpoint instance with the specified provisioner and server instance.
     *
     * @param serverInstance The server instance for managing server configurations and operations.
     */
    public OrderInfoEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
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
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));

        AcmeOrder order = AcmeOrder.getAcmeOrder(orderId, getServerInstance());
        List<AcmeOrderIdentifier> identifiers = order.getOrderIdentifiers();
        verifyIdentifiersPresent(orderId, identifiers);

        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, identifiers.getFirst().getOrder().getAccount(), gson, getServerInstance());
        getServerInstance().getNonceManager().checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        boolean allVerified = true;
        List<Identifier> identifierList = new ArrayList<>();
        List<String> authorizationsList = new ArrayList<>();

        for (AcmeOrderIdentifier identifier : identifiers) {
            if (identifier.getChallengeStatus() != AcmeStatus.VALID) {
                allVerified = false;
            }
            identifierList.add(new Identifier(identifier.getType(), identifier.getDataValue()));

            authorizationsList.add(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/authz/" + identifier.getAuthorizationId());
        }

        AcmeOrderResponse response = new AcmeOrderResponse();
        response.setExpires(DateTools.formatDateForACME(getOrderExpiration(order)));

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

        response.setFinalize(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/order/" + orderId + "/finalize");
        response.setCertificate(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/order/" + orderId + "/cert");
        response.setIdentifiers(identifierList);
        response.setAuthorizations(authorizationsList);

        ctx.json(response);
    }

    /**
     * Returns the expiration timestamp for the provided order.
     *
     * @param order The ACME order.
     * @return The expiration {@link Date} of the order.
     */
    Date getOrderExpiration(@NonNull AcmeOrder order) {
        return order.getExpires();
    }

    /**
     * Ensures that an order contains at least one identifier.
     *
     * @param orderId    The ID of the order being validated.
     * @param identifiers The list of identifiers associated with the order.
     * @throws ACMEResourceNotFoundException if no identifiers are present.
     */
    void verifyIdentifiersPresent(@NonNull String orderId, @NonNull List<AcmeOrderIdentifier> identifiers) {
        if (identifiers.isEmpty()) {
            log.error("Throwing API error: For the requested order {} was no identifier found", orderId);
            throw new ACMEResourceNotFoundException("For the requested order id was no identifier found");
        }
    }
}
