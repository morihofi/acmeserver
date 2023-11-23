package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;


import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.dataExtractor.CsrDataExtractor;
import de.morihofi.acmeserver.tools.certificate.CertTools;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.List;

public class FinalizeOrderEndpoint implements Handler {

    private final Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public FinalizeOrderEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String orderId = ctx.pathParam("orderId");

        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        JSONObject reqBodyPayloadObj = new JSONObject(acmeRequestBody.getDecodedPayload());
        JSONObject responseJSON = new JSONObject();

        String csr = reqBodyPayloadObj.getString("csr");

        // Check signature
        ACMEAccount account = Database.getAccountByOrderId(orderId);
        SignatureCheck.checkSignature(ctx, account, gson);
        // Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        List<String> csrDomainNames = CsrDataExtractor.getDomainsFromCSR(csr);
        if(csrDomainNames.isEmpty()){
            throw new ACMEBadCsrException("CSR does not contain any domain names");
        }

        List<ACMEIdentifier> identifiers = Database.getACMEIdentifiersByOrderId(orderId);
        for(String domain : csrDomainNames) {
            if(identifiers.stream().noneMatch(id -> domain.equals(id.getDataValue()))) {
                throw new ACMEBadCsrException("One or more CSR domains do not match the ACME identifiers");
            }
        }

        JSONArray identifiersArr = new JSONArray();
        JSONArray authorizationsArr = new JSONArray();
        for(ACMEIdentifier identifier : identifiers) {
            JSONObject identifierObj = new JSONObject();
            identifierObj.put("type", identifier.getType());
            identifierObj.put("value", identifier.getDataValue());
            identifiersArr.put(identifierObj);
            authorizationsArr.put(provisioner.getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());
        }

        try {
            byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
            PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
            PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());

            log.info("Creating Certificate for order \"" + orderId + "\" with DNS Names " + String.join(", ", csrDomainNames));
            X509Certificate acmeGeneratedCertificate = ServerCertificateGenerator.createServerCertificate(
                    provisioner.getIntermediateKeyPair(),
                    provisioner.getIntermediateCertificate().getEncoded(),
                    pkPemObject.getContent(),
                    csrDomainNames.toArray(new String[0]),
                    provisioner);

            BigInteger serialNumber = acmeGeneratedCertificate.getSerialNumber();
            String pemCertificate = PemUtil.certificateToPEM(acmeGeneratedCertificate.getEncoded());

            Timestamp expiresAt = new Timestamp(acmeGeneratedCertificate.getNotAfter().getTime());
            Timestamp issuedAt = new Timestamp(acmeGeneratedCertificate.getNotBefore().getTime());

            // Speichern des Zertifikats für jeden Identifier in der Datenbank
            for(ACMEIdentifier identifier : identifiers) {
                Database.storeCertificateInDatabase(orderId, identifier.getDataValue(), csr, pemCertificate, issuedAt, expiresAt, serialNumber);
            }

            ctx.header("Content-Type", "application/json");
            ctx.header("Replay-Nonce", Crypto.createNonce());
            ctx.header("Location", provisioner.getApiURL() + "/acme/order/" + orderId);

            responseJSON.put("status", "valid");
            responseJSON.put("expires", DateTools.formatDateForACME(expiresAt));
            responseJSON.put("finalize", provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");
            responseJSON.put("certificate", provisioner.getApiURL() + "/acme/order/" + orderId + "/cert");
            responseJSON.put("identifiers", identifiersArr);
            responseJSON.put("authorizations", authorizationsArr);

            ctx.result(responseJSON.toString());
        } catch (Exception ex) {
            log.error("Throwing API error: CSR processing error", ex);
            throw new ACMEBadCsrException("Unable to process requested CSR. Is the CSR valid? Otherwise try again later, if the problem persists contact support.");
        }
    }
}
