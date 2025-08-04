/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.certgine.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.api.endpoints.order.objects.AcmeOrderResponse;
import de.morihofi.certgine.cryptography.certificate.queue.CertificateIssuer;
import de.morihofi.certgine.cryptography.csr.CsrDataUtil;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.api.acme.dns.Identifier;
import de.morihofi.certgine.acme.api.endpoints.order.objects.FinalizeOrderRequestPayload;
import de.morihofi.certgine.acme.api.objects.ACMERequestBody;
import de.morihofi.certgine.types.database.entities.acme.*;
import de.morihofi.certgine.types.events.AcmeCertificateIssuanceRequestedEvent;
import de.morihofi.certgine.types.database.entities.acme.enums.AcmeOrderState;
import de.morihofi.certgine.types.database.entities.acme.enums.AcmeStatus;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadCsrException;
import de.morihofi.certgine.types.exception.exceptions.ACMEUnauthorizedException;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.server.StartupFlag;
import de.morihofi.certgine.utils.base64.Base64Tools;
import de.morihofi.certgine.utils.datetime.TimeTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.hibernate.Session;
import org.hibernate.Transaction;
import lombok.NonNull;

import java.util.List;

@Slf4j
public class FinalizeOrderEndpoint extends AbstractAcmeEndpoint {


    /**
     * ACME Endpoint for finalize an order
     */

    public FinalizeOrderEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

    void verifyAuthorizationsComplete(@NonNull List<AcmeOrderIdentifier> identifiers) throws ACMEUnauthorizedException {
        boolean allValid = identifiers.stream()
                .allMatch(id -> id.getChallengeStatus() == AcmeStatus.VALID);
        if (!allValid) {
            throw new ACMEUnauthorizedException("One or more identifiers are not validated yet");
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
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
            response.setExpires(TimeTools.formatInstantForAcme(order.getCertificateExpires().toInstant()));
            response.setIssued(TimeTools.formatInstantForAcme(order.getCertificateIssued().toInstant()));
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
