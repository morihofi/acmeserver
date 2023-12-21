package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.*;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

public class RevokeCertEndpoint implements Handler {

    private final Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());
    private final Gson gson;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RevokeCertEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
        this.gson = new Gson();
    }


    @Override
    public void handle(@NotNull Context ctx) throws Exception {


        //Parse request body
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        //Payload is Base64 Encoded
        JSONObject reqBodyPayloadObj = new JSONObject(acmeRequestBody.getDecodedPayload());
        JSONObject reqBodyProtectedObj = new JSONObject(acmeRequestBody.getDecodedProtected());

        //Check which method our server uses:
        //String accountId = null;
        if (reqBodyProtectedObj.has("kid")) {
            //Account Key Method
            //   accountId = getAccountIdFromProtected(reqBodyProtectedObj);
        } else {
            //if(reqBodyPayloadObj.has("jwk"))
            //Domain Key Method
            //Currently unsupported
            //TODO: Implement JWK Method
            throw new ACMEMalformedException("Method currently unsupported. Please use Account Key Method (kid)");
        }

        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        ACMEAccount account = Database.getAccount(accountId);
        //Check if account exists
        if (account == null) {
            log.error("Throwing API error: Account {} not found", accountId);
            throw new ACMEAccountNotFoundException("The account id was not found");
        }
        //Check signature
        SignatureCheck.checkSignature(ctx, accountId, gson);
        //Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());



        log.info("Account ID {} wants to revoke a certificate", accountId);

        String certificateBase64 = reqBodyPayloadObj.getString("certificate");

        //Parse certificate
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(certificateBase64))
        );

        // Check certificate (optional)
        // You can perform various checks here, e.g. validity date, issuer, etc.
        // Check issuer information
        log.debug("Issuer: {}", certificate.getIssuerX500Principal());

        // Read in root certificate
        X509Certificate intermediateCertificate = provisioner.getIntermediateCaCertificate();

        boolean isValid = true;
        // Validate given certificate against root certificate
        try {
            PublicKey intermediateCertificatePublicKey = intermediateCertificate.getPublicKey();
            certificate.verify(intermediateCertificatePublicKey);
            log.debug("Certificate is valid");
        } catch (Exception e) {
            log.error("Certificate is invalid or not from this CA", e);
            isValid = false;
        }


        // Validate validation date
        try {
            certificate.checkValidity(new Date());
            log.debug("Certificate date is valid");
        } catch (Exception e) {
            log.error("Certificate date is invalid");
            isValid = false;
        }

        if (!isValid) {
            throw new ACMEServerInternalException("Rejected: Certificate is invalid.");
        }


        // Extract serial number
        BigInteger serialNumber = certificate.getSerialNumber();


        //Get the identifier, where the certificate belongs to
        ACMEIdentifier identifier = Database.getACMEIdentifierCertificateSerialNumber(serialNumber);

        if(!identifier.getOrder().getAccount().getAccountId().equals(accountId)){
            throw new ACMEServerInternalException("Rejected: You cannot revoke a certificate, that belongs to another account.");
        }

        //Check if already revoked
        if(identifier.getRevokeStatusCode() != null && identifier.getRevokeTimestamp() != null){
            throw new ACMEAlreadyRevokedException("Error revoking certificate: The specified certificate is already revoked");
        }

        /*
        Reasons:
            0: Unspecified - No specific reason given.
            1: Key Compromise - The private key of the certificate has been compromised.
            2: CA Compromise - The CA that issued the certificate has been compromised.
            3: Affiliation Changed - The certificate holder has given up her affiliation with the organization that issued the certificate.
            4: Superseded - The certificate has been replaced by another certificate.
            5: Cessation Of Operation - The certificate holder has ceased business operations.
            6: Certificate Hold - The certificate is temporarily withheld.
            7: (Unspecified)
            8: Remove From CRL - The certificate was mistakenly placed on the revocation list.
         */
        int reason = reqBodyPayloadObj.getInt("reason");

        //Check reason code
        if (reason < 0 || reason > 8 || reason == 7) {
            throw new ACMEBadRevocationReasonException("Invalid revokation reason: " + reason);
        }

        log.info("Revoking certificate for reason {}", reason);

        //Revoke it
        Database.revokeCertificate(identifier, reason);


        ctx.status(200);
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Content-Length", "0");

        ctx.result();

    }
}
