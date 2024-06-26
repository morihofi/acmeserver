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

package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.exception.exceptions.*;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

/**
 * Endpoint for handling certificate revocation requests in the ACME server.
 * <p>
 * This class processes incoming ACME revocation requests, verifying the request signature,
 * validating the certificate, and revoking the certificate if all checks pass.
 * </p>
 */
public class RevokeCertEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructs a new RevokeCertEndpoint instance. This constructor initializes the endpoint with a specific Provisioner instance. It sets
     * up the necessary components for handling certificate revocation requests, including creating a new Gson instance for JSON
     * processing.
     *
     * @param provisioner The {@link Provisioner} instance used for managing certificate operations.
     * @param serverInstance The {@link ServerInstance} to use for this endpoint
     */
    public RevokeCertEndpoint(Provisioner provisioner, ServerInstance serverInstance) {
        super(provisioner, serverInstance);
    }


    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {

        // Payload is Base64 Encoded, so we get the decoded one
        JsonObject reqBodyPayloadObj = JsonParser.parseString(acmeRequestBody.getDecodedPayload()).getAsJsonObject();
        JsonObject reqBodyProtectedObj = JsonParser.parseString(acmeRequestBody.getDecodedProtected()).getAsJsonObject();

        // Check which method our server uses:
        // String accountId = null;
        if (reqBodyProtectedObj.has("kid")) {
            // Account Key Method
            //   accountId = getAccountIdFromProtected(reqBodyProtectedObj);
        } else {
            // if(reqBodyPayloadObj.has("jwk"))
            // Domain Key Method
            // Currently unsupported
            // TODO: Implement JWK Method
            throw new ACMEMalformedException("Method currently unsupported. Please use Account Key Method (kid)");
        }

        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        ACMEAccount account = ACMEAccount.getAccount(accountId, getServerInstance());
        // Check if account exists
        if (account == null) {
            LOG.error("Throwing API error: Account {} not found", accountId);
            throw new ACMEAccountNotFoundException("The account id was not found");
        }

        // Check signature and nonce
        performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

        LOG.info("Account ID {} wants to revoke a certificate", accountId);

        String certificateBase64 = reqBodyPayloadObj.get("certificate").getAsString();

        // Parse certificate
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(certificateBase64))
        );

        // Check certificate
        LOG.debug("Issuer: {}", certificate.getIssuerX500Principal());

        // Read in root certificate
        X509Certificate intermediateCertificate = provisioner.getIntermediateCaCertificate();

        boolean isValid = true;
        // Validate given certificate against root certificate
        try {
            PublicKey intermediateCertificatePublicKey = intermediateCertificate.getPublicKey();
            certificate.verify(intermediateCertificatePublicKey);
            LOG.debug("Certificate is valid");
        } catch (Exception e) {
            LOG.error("Certificate is invalid or not from this CA", e);
            isValid = false;
        }

        // Validate validation date
        try {
            certificate.checkValidity(new Date());
            LOG.debug("Certificate date is valid");
        } catch (Exception e) {
            LOG.error("Certificate date is invalid");
            isValid = false;
        }

        if (!isValid) {
            throw new ACMEServerInternalException("Rejected: Certificate is invalid.");
        }

        // Extract serial number
        BigInteger serialNumber = certificate.getSerialNumber();

        // Get the identifier, where the certificate belongs to
        ACMEOrder order = ACMEOrder.getACMEOrderCertificateSerialNumber(serialNumber, getServerInstance());

        if (!order.getAccount().getAccountId().equals(accountId)) {
            throw new ACMEServerInternalException("Rejected: You cannot revoke a certificate, that belongs to another account.");
        }

        // Check if already revoked
        if (order.getRevokeStatusCode() != null && order.getRevokeTimestamp() != null) {
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
        int reason = reqBodyPayloadObj.get("reason").getAsInt();

        // Check reason code
        if (reason < 0 || reason > 8 || reason == 7) {
            throw new ACMEBadRevocationReasonException("Invalid revokation reason: " + reason);
        }

        LOG.info("Revoking certificate for reason {}", reason);

        // Revoke it
        ACMEOrder.revokeCertificate(order, reason, getServerInstance());

        ctx.status(200);
        ctx.header("Replay-Nonce", Crypto.createNonce(getServerInstance()));
        ctx.header("Content-Length", "0");

        ctx.result();
    }
}
