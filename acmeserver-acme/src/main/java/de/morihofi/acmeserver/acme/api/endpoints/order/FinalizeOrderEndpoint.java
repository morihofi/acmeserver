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
import de.morihofi.acmeserver.acme.api.endpoints.order.objects.AcmeOrderResponse;
import de.morihofi.acmeserver.cryptography.certificate.queue.CertificateIssuer;
import de.morihofi.acmeserver.cryptography.csr.CsrDataUtil;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import de.morihofi.acmeserver.acme.api.endpoints.order.objects.FinalizeOrderRequestPayload;
import de.morihofi.acmeserver.acme.api.objects.ACMERequestBody;
import de.morihofi.acmeserver.types.events.AcmeCertificateIssuanceRequestedEvent;
import de.morihofi.acmeserver.types.database.entities.*;
import de.morihofi.acmeserver.types.database.enums.AcmeOrderState;
import de.morihofi.acmeserver.types.database.enums.AcmeStatus;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.types.exception.exceptions.ACMEUnauthorizedException;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.types.server.StartupFlag;
import de.morihofi.acmeserver.utils.base64.Base64Tools;
import de.morihofi.acmeserver.utils.datetime.DateTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class FinalizeOrderEndpoint extends AbstractAcmeEndpoint {


    /**
     * ACME Endpoint for finalize an order
     */

    public FinalizeOrderEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

    void verifyAuthorizationsComplete(@NotNull List<AcmeOrderIdentifier> identifiers) throws ACMEUnauthorizedException {
        boolean allValid = identifiers.stream()
                .allMatch(id -> id.getChallengeStatus() == AcmeStatus.VALID);
        if (!allValid) {
            throw new ACMEUnauthorizedException("One or more identifiers are not validated yet");
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void handleRequest(@NotNull HandlerContext ctx, @NotNull AcmeProvisioner provisioner, @NotNull Gson gson, @NotNull ACMERequestBody acmeRequestBody) throws Exception {
        String orderId = ctx.pathParam("orderId");

        AcmeOrder order = AcmeOrder.getAcmeOrder(orderId, getServerInstance());
        AcmeAccount account = order.getAccount();

        // Check signature and nonce
        assert account != null;
        performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

        // After check parse payload
        FinalizeOrderRequestPayload reqBodyPayloadObj =
                gson.fromJson(acmeRequestBody.getDecodedPayload(), FinalizeOrderRequestPayload.class);
        String csr = reqBodyPayloadObj.getCsr();

        // Get our ACME identifiers
        List<AcmeOrderIdentifier> identifiers = AcmeOrder.getAcmeOrder(orderId, getServerInstance()).getOrderIdentifiers();

        // Ensure all authorizations are completed before processing the CSR
        verifyAuthorizationsComplete(identifiers);

        // We just use the verification, that throws exceptions, here not the resulting identifiers
        CsrDataUtil.getCsrIdentifiersAndVerifyWithIdentifiers(csr, identifiers);

        // Convert AcmeOrderIdentifier into simple identifier
        List<Identifier> identifierList = identifiers.stream()
                .map(id -> new Identifier(id.getType(), id.getDataValue()))
                .toList();

        // One authorization per identifier
        List<String> authorizationsList = identifiers.stream()
                .map(AcmeOrderIdentifier -> provisioner.getAcmeApiURL(getServerInstance()) + "/acme/authz/" + AcmeOrderIdentifier.getAuthorizationId())
                .toList();

        try {

            // Decode the CSR from the Request, we're just try to decode it to verify it is vaild
            byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);

            if (csrBytes.length == 0) {
                throw new ACMEBadCsrException("CSR bytes are 0 -> Invalid CSR");
            }

            // Try if we can deserialize the CSR
            PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
            new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());
        } catch (Exception ex) {
            throw new ACMEBadCsrException("Unable to process requested CSR. Is the CSR valid and deserializable?");
        }

        AcmeOrderResponse response = new AcmeOrderResponse();

        if (order.getCertificatePem() == null && order.getCertificateCSR() == null) {

            try (Session session = getServerInstance().getDatabaseSession()) {

                // Save CSR in Database (and mark it that it needs a certificate)
                Transaction transaction = session.beginTransaction();

                order.setCertificateCSR(csr);
                order.setOrderState(AcmeOrderState.NEED_A_CERTIFICATE);
                session.merge(order);

                transaction.commit();

                if (getServerInstance().getStartupFlags().contains(StartupFlag.USE_ASYNC_CERTIFICATE_ISSUING)) {
                    // Use async certificate issuing via event bus
                    log.info("Saved CSR for order {} in database", order.getOrderId());
                    getServerInstance().getEventBus().publish(new AcmeCertificateIssuanceRequestedEvent(order));
                    response.setStatus(AcmeStatus.PROCESSING.getRfcName());
                } else {
                    CertificateIssuer.generateCertificateForOrder(order, getServerInstance().getCryptoStoreManager(),
                            session, getServerInstance()); // also resets need certificate status

                    // Valid, cause due we generated the certificate in the request, we have now a certificate available
                    response.setStatus(AcmeStatus.VALID.getRfcName());
                }
            } catch (Exception e) {
                log.error("Unable to process CSR for order {} and save in database", order.getOrderId(), e);
            }
        } else {
            // We have a certificate

            response.setStatus(AcmeStatus.VALID.getRfcName());
            response.setExpires(DateTools.formatDateForACME(order.getCertificateExpires()));
            response.setIssued(DateTools.formatDateForACME(order.getCertificateIssued()));
        }

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));
        ctx.header("Location", provisioner.getAcmeApiURL(getServerInstance()) + "/acme/order/" + orderId);

        response.setFinalize(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/order/" + orderId + "/finalize");
        response.setCertificate(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/order/" + orderId + "/cert");
        response.setIdentifiers(identifierList);
        response.setAuthorizations(authorizationsList);

        ctx.json(response);
    }
}
