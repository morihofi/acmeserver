package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;


import com.google.gson.Gson;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.ACMEOrderResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.FinalizeOrderRequestPayload;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.queue.CertificateIssuer;
import de.morihofi.acmeserver.database.AcmeOrderState;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import de.morihofi.acmeserver.tools.certificate.dataExtractor.CsrDataUtil;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Objects;

public class FinalizeOrderEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    public final Logger log = LogManager.getLogger(getClass());

    /**
     * ACME Endpoint for finalize an order
     *
     * @param provisioner Provisioner instance
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public FinalizeOrderEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String orderId = ctx.pathParam("orderId");

        ACMEOrder order = ACMEOrder.getACMEOrder(orderId);
        ACMEAccount account = order.getAccount();

        // Check signature and nonce
        assert account != null;
        performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

        // After check parse payload
        FinalizeOrderRequestPayload reqBodyPayloadObj = gson.fromJson(acmeRequestBody.getDecodedPayload(), FinalizeOrderRequestPayload.class);
        String csr = reqBodyPayloadObj.getCsr();

        // Get our ACME identifiers
        List<ACMEOrderIdentifier> identifiers = ACMEOrder.getACMEOrder(orderId).getOrderIdentifiers();

        //We just use the verification, that throws exceptions, here not the resulting identifiers
        CsrDataUtil.getCsrIdentifiersAndVerifyWithIdentifiers(csr, identifiers);

        // Convert ACMEOrderIdentifier into simple identifier
        List<Identifier> identifierList = identifiers.stream()
                .map(id -> new Identifier(id.getType(), id.getDataValue()))
                .toList();

        // One authorization per identifier
        List<String> authorizationsList = identifiers.stream()
                .map(acmeOrderIdentifier -> provisioner.getApiURL() + "/acme/authz/" + acmeOrderIdentifier.getAuthorizationId())
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

        ACMEOrderResponse response = new ACMEOrderResponse();

        if (order.getCertificatePem() == null && order.getCertificateCSR() == null) {

            try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {

                // Save CSR in Database (and mark it that it needs a certificate)
                Transaction transaction = session.beginTransaction();

                order.setCertificateCSR(csr);
                order.setOrderState(AcmeOrderState.NEED_A_CERTIFICATE);
                session.merge(order);

                transaction.commit();

                if (Main.getServerOptions().contains(Main.SERVER_OPTION.USE_ASYNC_CERTIFICATE_ISSUING)) {
                    //Use async certificate issuing


                    log.info("Saved CSR for order {} in database", order.getOrderId());

                    //Set response, that our certificate is processing in separate thread
                    response.setStatus(AcmeStatus.PROCESSING.getRfcName());
                } else {

                    CertificateIssuer.generateCertificateForOrder(order, provisioner.getCryptoStoreManager(), session); //also resets need certificate status

                    //Valid, cause due we generated the certificate in the request, we have now a certificate available
                    response.setStatus(AcmeStatus.VALID.getRfcName());
                }


            } catch (Exception e) {
                log.error("Unable to process CSR for order {} and save in database", order.getOrderId(), e);
            }
        } else {
            //We have a certificate

            response.setStatus(AcmeStatus.VALID.getRfcName());
            response.setExpires(DateTools.formatDateForACME(order.getCertificateExpires()));
            response.setIssued(DateTools.formatDateForACME(order.getCertificateIssued()));
        }


        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Location", provisioner.getApiURL() + "/acme/order/" + orderId);

        response.setFinalize(provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");
        response.setCertificate(provisioner.getApiURL() + "/acme/order/" + orderId + "/cert");
        response.setIdentifiers(identifierList);
        response.setAuthorizations(authorizationsList);

        ctx.json(response);

    }


}
