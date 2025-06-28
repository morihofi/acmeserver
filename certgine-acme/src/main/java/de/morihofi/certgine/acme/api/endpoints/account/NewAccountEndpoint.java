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

package de.morihofi.certgine.acme.api.endpoints.account;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.certgine.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.api.endpoints.account.objects.ACMEAccountRequestPayload;
import de.morihofi.certgine.acme.api.endpoints.account.objects.AccountResponse;
import de.morihofi.certgine.acme.api.endpoints.account.objects.ExternalAccountBinding;
import de.morihofi.certgine.acme.api.objects.ACMERequestBody;

import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.acme.enums.AcmeStatus;
import de.morihofi.certgine.types.database.entities.acme.AcmeAccount;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.acme.AcmeExternalAccountBinding;
import de.morihofi.certgine.types.database.entities.acme.HttpNonces;
import de.morihofi.certgine.types.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.certgine.types.exception.exceptions.ACMEUserActionRequiredException;
import de.morihofi.certgine.types.exception.exceptions.ACMEServerInternalException;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.http.HttpHeaderUtil;
import de.morihofi.certgine.utils.regex.EmailValidator;
import de.morihofi.certgine.types.events.AcmeAccountCreatedEvent;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;
import lombok.NonNull;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.HmacKey;

import java.util.Base64;

import java.util.List;
import java.util.UUID;

/**
 * Endpoint for handling the creation of new ACME accounts.
 */
@Slf4j
public class NewAccountEndpoint extends AbstractAcmeEndpoint {

    /**
     * Constructs a new instance of the NewAccountEndpoint class.
     *
     * @param serverInstance The server instance running this endpoint.
     */
    public NewAccountEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

    /**
     * Handles the creation of a new ACME account.
     *
     * @param ctx             The context of the request.
     * @param provisioner     The provisioner managing this request.
     * @param gson            The Gson instance for JSON parsing.
     * @param acmeRequestBody The body of the ACME request.
     * @throws Exception If an error occurs while handling the request.
     */
    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
        // Check nonce
        getServerInstance().getNonceManager().checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        // Deserialize payload and protected objects
        ACMEAccountRequestPayload payload = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMEAccountRequestPayload.class);

        // Check terms of service agreement
        if (!payload.isTermsOfServiceAgreed()) {
            String tosUrl = "about:blank";
            if (provisioner.getMeta().getTos() != null) {
                tosUrl = provisioner.getMeta().getTos().trim();
            }
            ctx.header("Link", HttpHeaderUtil.buildLinkHeaderValue(tosUrl, "terms-of-service"));
            throw new ACMEUserActionRequiredException("User must agree to terms of service");
        }

        // Validate email addresses
        List<String> emails = payload.getContact();
        if (emails != null) {
            for (String email : emails) {
                email = email.replace("mailto:", "");
                if (!EmailValidator.isValidEmail(email) || email.split("@")[0].equals("localhost")) {
                    log.error("E-Mail validation failed for email {}", email);
                    throw new ACMEInvalidContactException("Mail validation failed for email " + email);
                }
            }
        }

        // Create new account in database
        String accountId = UUID.randomUUID().toString();

        // Parse the JSON string to a JsonElement or directly to JsonObject
        JsonObject decodedProtectedJsonObject = JsonParser.parseString(acmeRequestBody.getDecodedProtected()).getAsJsonObject();

        // Extract the "jwk" JsonObject as a string
        String jwkString = decodedProtectedJsonObject.getAsJsonObject("jwk").toString();

        AcmeExternalAccountBinding bindingUsed = validateExternalAccountBinding(payload.getExternalAccountBinding(),
                provisioner, jwkString);

        PublicJsonWebKey publicJsonWebKey;
        try {
            publicJsonWebKey = (PublicJsonWebKey) JsonWebKey.Factory.newJwk(jwkString);
        } catch (JoseException e) {
            log.error("Error parsing JWK", e);
            throw new ACMEServerInternalException("Error parsing JWK: " + e.getMessage());
        }

        String publicKeyPEM = PemUtil.convertToPem(publicJsonWebKey.getPublicKey());

        try (Session session = getServerInstance().getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();
            AcmeAccount account = new AcmeAccount();
            account.setAccountId(accountId);
            account.setPublicKeyPEM(publicKeyPEM);
            account.setEmails(emails);
            account.setDeactivated(false);
            account.setAcmeProvisioner(provisioner);
            if (bindingUsed != null) {
                account.setExternalAccountBinding(bindingUsed);
            }
            session.persist(account);
            transaction.commit();
            log.info("New ACME account created with account id {}", accountId);
            getServerInstance().getEventBus().publish(new AcmeAccountCreatedEvent(account));
        } catch (Exception e) {
            log.error("Unable to create new ACME account", e);
            throw new ACMEServerInternalException(e.getMessage());
        }

        // Construct response
        String nonce = HttpNonces.createNonce(getServerInstance());
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getAcmeApiURL(getServerInstance()) + "/acme/acct/" + accountId);
        ctx.header("Replay-Nonce", nonce);
        ctx.status(201); // Created

        AccountResponse response = new AccountResponse();
        response.setStatus(AcmeStatus.VALID.getRfcName());
        response.setContact(emails);
        response.setOrders(provisioner.getAcmeApiURL(getServerInstance()) + "/acme/acct/" + accountId + "/orders");

        ctx.json(response);
    }

    /**
     * Validates the provided external account binding and returns the binding used.
     *
     * @param eab         ExternalAccountBinding from the request
     * @param provisioner Provisioner handling the request
     * @param jwkString   JWK string of the new account
     * @return the used {@link AcmeExternalAccountBinding} or {@code null}
     * @throws ACMEUserActionRequiredException if validation fails
     */
    private AcmeExternalAccountBinding validateExternalAccountBinding(ExternalAccountBinding eab,
                                                                      AcmeProvisioner provisioner,
                                                                      String jwkString) throws ACMEUserActionRequiredException {
        if (provisioner.isExternalAccountBindingRequired() && eab == null) {
            throw new ACMEUserActionRequiredException("External Account Binding required");
        }

        if (eab == null) {
            return null;
        }

        String serialized = eab.getProtectedHeader() + "." + eab.getPayload() + "." + eab.getSignature();
        JsonWebSignature eabJws = new JsonWebSignature();
        try {
            eabJws.setCompactSerialization(serialized);
        } catch (JoseException e) {
            throw new ACMEUserActionRequiredException("Invalid external account binding signature");
        }

        String kid = eabJws.getKeyIdHeaderValue();
        AcmeExternalAccountBinding key = AcmeExternalAccountBinding.getForKid(getServerInstance(), kid);
        if (key == null) {
            throw new ACMEUserActionRequiredException("Unknown external account binding key");
        }

        eabJws.setKey(new HmacKey(Base64.getDecoder().decode(key.getHmacKey())));
        try {
            if (!eabJws.verifySignature()) {
                throw new ACMEUserActionRequiredException("Invalid external account binding signature");
            }
        } catch (JoseException e) {
            throw new ACMEUserActionRequiredException("Invalid external account binding signature");
        }

        String payload;
        try {
            payload = eabJws.getPayload();
        } catch (JoseException e) {
            throw new ACMEUserActionRequiredException("Invalid external account binding signature");
        }

        if (!payload.equals(jwkString)) {
            throw new ACMEUserActionRequiredException("External account binding payload mismatch");
        }
        return key;
    }
}
