package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;


import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.ACMEOrderResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.FinalizeOrderRequestPayload;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.dataExtractor.CsrDataExtractor;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.List;

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

        ACMEAccount account = Database.getAccountByOrderId(orderId);
        // Check signature and nonce
        assert account != null;
        performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

        // After check parse payload
        FinalizeOrderRequestPayload reqBodyPayloadObj = gson.fromJson(acmeRequestBody.getDecodedPayload(), FinalizeOrderRequestPayload.class);
        String csr = reqBodyPayloadObj.getCsr();

        // Extract CSR Domain Names
        List<String> csrDomainNames = CsrDataExtractor.getDomainsAndIPsFromCSR(csr);
        if (csrDomainNames.isEmpty()) {
            throw new ACMEBadCsrException("CSR does not contain any domain names");
        }

        // Get our ACME identifiers
        List<ACMEIdentifier> identifiers = Database.getACMEIdentifiersByOrderId(orderId);
        for (String domain : csrDomainNames) {
            if (identifiers.stream().noneMatch(id -> domain.equals(id.getDataValue()))) {
                throw new ACMEBadCsrException("One or more CSR domains do not match the ACME identifiers");
            }
        }

        List<Identifier> identifierList = identifiers.stream()
                .map(id -> new Identifier(id.getType(), id.getDataValue()))
                .toList();

        List<String> authorizationsList = identifiers.stream()
                .map(id -> provisioner.getApiURL() + "/acme/authz/" + id.getAuthorizationId())
                .toList();

        try {
            // Decode the CSR from the Request
            byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
            PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
            PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());

            /*
                We just use the DNS Domain Names (Subject Alternative Name) and the public key of the CSR. We're not using the Basic Constrain etc.
             */

            log.info("Creating Certificate for order \"{}\" with DNS Names {}", orderId, String.join(", ", csrDomainNames));
            X509Certificate acmeGeneratedCertificate = ServerCertificateGenerator.createServerCertificate(
                    provisioner.getIntermediateCaKeyPair(),
                    provisioner.getIntermediateCaCertificate(),
                    pkPemObject.getContent(),
                    csrDomainNames.toArray(new String[0]),
                    provisioner);

            BigInteger serialNumber = acmeGeneratedCertificate.getSerialNumber();
            String pemCertificate = PemUtil.certificateToPEM(acmeGeneratedCertificate.getEncoded());

            Timestamp expiresAt = new Timestamp(acmeGeneratedCertificate.getNotAfter().getTime());
            Timestamp issuedAt = new Timestamp(acmeGeneratedCertificate.getNotBefore().getTime());

            // Saving the certificate for each identifier in the database
            for (ACMEIdentifier identifier : identifiers) {
                Database.storeCertificateInDatabase(orderId, identifier.getDataValue(), csr, pemCertificate, issuedAt, expiresAt, serialNumber);
            }

            ctx.header("Content-Type", "application/json");
            ctx.header("Replay-Nonce", Crypto.createNonce());
            ctx.header("Location", provisioner.getApiURL() + "/acme/order/" + orderId);

            ACMEOrderResponse response = new ACMEOrderResponse();
            response.setStatus("valid");
            response.setExpires(DateTools.formatDateForACME(expiresAt));
            response.setIssued(DateTools.formatDateForACME(issuedAt));
            response.setFinalize(provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");
            response.setCertificate(provisioner.getApiURL() + "/acme/order/" + orderId + "/cert");
            response.setIdentifiers(identifierList);
            response.setAuthorizations(authorizationsList);

            ctx.result(gson.toJson(response));
        } catch (Exception ex) {
            log.error("Throwing API error: CSR processing error", ex);
            throw new ACMEBadCsrException("Unable to process requested CSR. Is the CSR valid? Otherwise try again later, if the problem persists contact support. If you're the server administrator, check the logs for more information.");
        }
    }


}
