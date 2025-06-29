/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.certgine.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.security.SignatureCheck;
import de.morihofi.certgine.acme.api.objects.ACMERequestBody;

import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.acme.AcmeAccount;
import de.morihofi.certgine.types.database.entities.acme.AcmeOrder;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.acme.HttpNonces;
import de.morihofi.certgine.types.events.AcmeCertificateRevokedEvent;
import de.morihofi.certgine.types.exception.exceptions.*;
import de.morihofi.certgine.types.intf.IServerInstance;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import lombok.NonNull;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
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
@Slf4j
public class RevokeCertEndpoint extends AbstractAcmeEndpoint {

    /**
     * Constructs a new RevokeCertEndpoint instance. This constructor initializes the endpoint with a specific Provisioner instance. It sets
     * up the necessary components for handling certificate revocation requests, including creating a new Gson instance for JSON
     * processing.
     *
     * @param serverInstance The {@link IServerInstance} to use for this endpoint
     */
    public RevokeCertEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }


    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {

        // Payload is Base64 Encoded, so we get the decoded one
        JsonObject reqBodyPayloadObj = JsonParser.parseString(acmeRequestBody.getDecodedPayload()).getAsJsonObject();
        JsonObject reqBodyProtectedObj = JsonParser.parseString(acmeRequestBody.getDecodedProtected()).getAsJsonObject();

        // Check which method our server uses:
        boolean usingJwkMethod = reqBodyProtectedObj.has("jwk");

        // Else domain key
        String accountId = null;
        AcmeAccount account = null;
        PublicKey jwkPublicKey = null;

        if (usingJwkMethod) {
            // Domain Key Method
            String jwkStr = reqBodyProtectedObj.getAsJsonObject("jwk").toString();
            PublicJsonWebKey jwk;
            try {
                jwk = (PublicJsonWebKey) JsonWebKey.Factory.newJwk(jwkStr);
            } catch (JoseException e) {
                throw new ACMEServerInternalException("Error parsing JWK: " + e.getMessage());
            }

            jwkPublicKey = jwk.getPublicKey();

            SignatureCheck.checkSignature(ctx, jwkPublicKey, gson);
            getServerInstance().getNonceManager().checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

            log.info("Certificate key wants to revoke a certificate");
        } else {
            // Account Key Method
            accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
            account = AcmeAccount.getAccount(accountId, getServerInstance());
            // Check if account exists
            if (account == null) {
                log.error("Throwing API error: Account {} not found", accountId);
                throw new ACMEAccountNotFoundException("The account id was not found");
            }

            // Check signature and nonce
            performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

            log.info("Account ID {} wants to revoke a certificate", accountId);
        }

        String certificateBase64 = reqBodyPayloadObj.get("certificate").getAsString();

        // Parse certificate
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(certificateBase64))
        );

        // Check certificate
        log.debug("Issuer: {}", certificate.getIssuerX500Principal());

        if (usingJwkMethod && !certificate.getPublicKey().equals(jwkPublicKey)) {
            throw new ACMEMalformedException("JWK does not match certificate public key");
        }

        // Read in root certificate
        X509Certificate intermediateCertificate = getServerInstance().getCryptoStoreManager().getIntermediateCertificate(provisioner.getInternalUuid());

        boolean isValid = true;
        // Validate given certificate against root certificate
        try {
            PublicKey intermediateCertificatePublicKey = intermediateCertificate.getPublicKey();
            certificate.verify(intermediateCertificatePublicKey);
            log.debug("Certificate is valid");
        } catch (
                Exception e) {
            log.error("Certificate is invalid or not from this CA", e);
            isValid = false;
        }

        // Validate validation date
        try {
            certificate.checkValidity(new Date());
            log.debug("Certificate date is valid");
        } catch (
                Exception e) {
            log.error("Certificate date is invalid");
            isValid = false;
        }

        if (!isValid) {
            throw new ACMEServerInternalException("Rejected: Certificate is invalid.");
        }

        // Extract serial number
        BigInteger serialNumber = certificate.getSerialNumber();

        // Get the identifier, where the certificate belongs to
        AcmeOrder order = AcmeOrder.getAcmeOrderCertificateSerialNumber(serialNumber, getServerInstance());

        if (!usingJwkMethod && !order.getAccount().getAccountId().equals(accountId)) {
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

        log.info("Revoking certificate for reason {}", reason);

        // Revoke it
        AcmeOrder.revokeCertificate(order, reason, getServerInstance());
        getServerInstance().getEventBus().publish(new AcmeCertificateRevokedEvent(order));

        ctx.status(HttpURLConnection.HTTP_OK);
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));
        ctx.header("Content-Length", "0");

        ctx.result();
    }
}
