/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.ACMEOrderResponse;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderInfoEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    public OrderInfoEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) {
        String orderId = ctx.pathParam("orderId");

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce());

        ACMEOrder order = ACMEOrder.getACMEOrder(orderId);
        List<ACMEOrderIdentifier> identifiers = order.getOrderIdentifiers();
        if (identifiers.isEmpty()) {
            throw new IllegalArgumentException("Identifiers empty, FIXME");
        }

        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, identifiers.get(0).getOrder().getAccount(), gson);
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        boolean allVerified = true;
        List<Identifier> identifierList = new ArrayList<>();
        List<String> authorizationsList = new ArrayList<>();
        Date orderExpires = new Date();

        for (ACMEOrderIdentifier identifier : identifiers) {
            if (identifier.getChallengeStatus() != AcmeStatus.VALID) {
                allVerified = false;
            }
            identifierList.add(new Identifier(identifier.getType(), identifier.getDataValue()));

            authorizationsList.add(provisioner.getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());
        }

        ACMEOrderResponse response = new ACMEOrderResponse();
        response.setExpires(DateTools.formatDateForACME(orderExpires));

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

        response.setFinalize(provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");
        response.setCertificate(provisioner.getApiURL() + "/acme/order/" + orderId + "/cert");
        response.setIdentifiers(identifierList);
        response.setAuthorizations(authorizationsList);

        ctx.json(response);
    }
}
