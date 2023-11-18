package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;


import com.google.gson.Gson;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.tools.CertTools;
import de.morihofi.acmeserver.tools.Crypto;
import de.morihofi.acmeserver.tools.DateTools;
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

public class FinalizeOrderEndpoint implements Handler {

    private Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public FinalizeOrderEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {

        String orderId = ctx.pathParam("orderId");

        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        //Check signature
        JSONObject reqBodyPayloadObj = new JSONObject(acmeRequestBody.getDecodedPayload());
        JSONObject responseJSON = new JSONObject();

        String csr = reqBodyPayloadObj.getString("csr");

        //Check signature
        ACMEAccount account = Database.getAccountByOrderId(orderId);
        SignatureCheck.checkSignature(ctx, account, gson);

        //TODO: Check if CSR Domain-names are matching with requested and checked Domain-names

        ACMEIdentifier identifier = Database.getACMEIdentifierByOrderId(orderId);

        JSONObject identifiersObj = new JSONObject();
        identifiersObj.put("type", identifier.getType());
        identifiersObj.put("value", identifier.getDataValue());

        JSONArray authorizationsArr = new JSONArray();
        authorizationsArr.put(provisioner.getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());

        try {


            byte[] csrBytes = CertTools.decodeBase64URLAsBytes(csr);
            PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
            PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());
            //RSAKeyParameters pubKey = (RSAKeyParameters) PublicKeyFactory.createKey(csrObj.getSubjectPublicKeyInfo());

            log.info("Creating Certificate for order \"" + orderId + "\" with DNS Name \"" + identifier.getDataValue() + "\"");
            X509Certificate acmeGeneratedCertificate = CertTools.createServerCertificate(provisioner.getIntermediateKeyPair(), provisioner.getIntermediateCertificate() .getEncoded(), pkPemObject.getContent(), new String[]{identifier.getDataValue()}, provisioner.getGeneratedCertificateExpiration());

            BigInteger serialNumber = acmeGeneratedCertificate.getSerialNumber();
            String pemCertificate = CertTools.certificateToPEM(acmeGeneratedCertificate.getEncoded());

            Timestamp expiresAt = new Timestamp(acmeGeneratedCertificate.getNotAfter().getTime());
            Timestamp issuedAt = new Timestamp(acmeGeneratedCertificate.getNotBefore().getTime());

            Database.storeCertificateInDatabase(orderId, identifier.getDataValue(), csr, pemCertificate, issuedAt, expiresAt, serialNumber);


            ctx.header("Content-Type", "application/json");
            ctx.header("Replay-Nonce", Crypto.createNonce());
            ctx.header("Location", provisioner.getApiURL() + "/acme/order/" + orderId);


            responseJSON.put("status", "valid");
            responseJSON.put("expires", DateTools.formatDateForACME(expiresAt));
            responseJSON.put("finalize", provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");
            responseJSON.put("certificate", provisioner.getApiURL() + "/acme/order/" + orderId + "/cert"); //Temporary fix
            //responseJSON.put("certificate", getApiURL() + "/acme/cert/" + identifier.getCertificateId());
            responseJSON.put("authorizations", authorizationsArr);


            ctx.result(responseJSON.toString());

        } catch (Exception ex) {
            log.error("Throwing API error: CSR processing error", ex);
            throw new ACMEBadCsrException("Unable process requested CSR. Is the CSR valid? Otherwise try again later, if the problem persists contact support.");
        }

    }
}
