package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;


import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.ACMEOrderResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.FinalizeOrderRequestPayload;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.AcmeOrderState;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import de.morihofi.acmeserver.tools.certificate.dataExtractor.CsrDataExtractor;
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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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

        ACMEOrder order = Database.getACMEOrder(orderId);
        ACMEAccount account = order.getAccount();

        // Check signature and nonce
        assert account != null;
        performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

        // After check parse payload
        FinalizeOrderRequestPayload reqBodyPayloadObj = gson.fromJson(acmeRequestBody.getDecodedPayload(), FinalizeOrderRequestPayload.class);
        String csr = reqBodyPayloadObj.getCsr();

        // Get our ACME identifiers
        List<ACMEOrderIdentifier> identifiers = Database.getACMEOrder(orderId).getOrderIdentifiers();

        //We just use the verification, that throws exceptions, here not the resulting identifiers
        getCsrIdentifiersAndVerifyWithIdentifiers(csr, identifiers);

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
            PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());
        } catch (Exception ex) {
            throw new ACMEBadCsrException("Unable to process requested CSR. Is the CSR valid and deserializable?");
        }

        ACMEOrderResponse response = new ACMEOrderResponse();

        if (order.getCertificatePem() == null) {
            try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {


                // Save CSR in Database (and mark it that it needs a certificate)
                    Transaction transaction = session.beginTransaction();

                    order.setCertificateCSR(csr);
                    order.setOrderState(AcmeOrderState.NEED_A_CERTIFICATE); //Set it back to idle
                    session.merge(order);

                    transaction.commit();

                    log.info("Saved CSR for order {} in database", order.getOrderId());


                    //Set response, that our certificate is processing
                    response.setStatus(AcmeStatus.PROCESSING.getRfcName());

            } catch (Exception e) {
                log.error("Unable to store CSR for order {} in database", order.getOrderId(), e);
            }
        }else {
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

        ctx.result(gson.toJson(response));

    }

    @NotNull
    public static List<Identifier> getCsrIdentifiersAndVerifyWithIdentifiers(String csr, List<ACMEOrderIdentifier> identifiers) throws IOException {
        // Extract CSR Domain Names
        List<Identifier> csrDomainNames = CsrDataExtractor.getDomainsAndIPsFromCSR(csr);
        if (csrDomainNames.isEmpty()) {
            throw new ACMEBadCsrException("CSR does not contain any domain names");
        }


        if (!identifiers.stream().allMatch(acmeOrderIdentifier -> acmeOrderIdentifier.getChallengeStatus() == AcmeStatus.VALID)) {
            throw new ACMEServerInternalException("Not all ACME Identifiers were validated");
        }

        for (String domain : csrDomainNames.stream()
                .map(identifier -> identifier.getValue())
                .toList()
        ) {
            if (identifiers.stream().noneMatch(id -> domain.equals(id.getDataValue()))) {
                throw new ACMEBadCsrException("One or more CSR domains do not match the ACME identifiers");
            }
        }
        return csrDomainNames;
    }


}
