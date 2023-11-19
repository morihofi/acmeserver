package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.CertTools;
import de.morihofi.acmeserver.tools.Crypto;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

public class RevokeCertEndpoint implements Handler {

    private Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public RevokeCertEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }


    @Override
    public void handle(@NotNull Context ctx) throws Exception {


        //Parse request body
        Gson gson = new Gson();
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
            throw new ACMEMalformedException("Method currently unsupported. Please use Account Key Method (kid)");
        }

        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        ACMEAccount account = Database.getAccount(accountId);
        //Check if account exists
        if (account == null) {
            log.error("Throwing API error: Account \"" + accountId + "\" not found");
            throw new ACMEAccountNotFoundException("The account id was not found");
        }
        //Check signature
        SignatureCheck.checkSignature(ctx, accountId, gson);
        //Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());



        log.info("Account ID \"" + accountId + "\" wants to revoke a certificate");

        System.out.println(reqBodyProtectedObj.toString(4));

        String certificateBase64 = reqBodyPayloadObj.getString("certificate");

        //Parse certificate
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(certificateBase64))
        );

        // Zertifikat prüfen (optional)
        // Hier können Sie verschiedene Prüfungen durchführen, z.B. Gültigkeitsdatum, Aussteller usw.
        // Ausstellerinformationen prüfen
        log.debug("Issuer: " + certificate.getIssuerX500Principal());

        // Root-Zertifikat einlesen
        X509Certificate intermediateCertificate = provisioner.getIntermediateCertificate();

        boolean isValid = true;
        // Zertifikat gegen Root-Zertifikat validieren
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
            throw new ACMEMalformedException("Invalid revoke reason");
        }

        log.info("Revoke certificate for reason " + reason);

        Database.revokeCertificate(identifier, reason);


        ctx.status(200);
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");
        ctx.header("Replay-Nonce", Crypto.createNonce());
        ctx.header("Content-Length", "0");

        ctx.result();

    }
}
