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

package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.ACMEAccountRequestPayload;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.AccountResponse;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.regex.EmailValidation;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Endpoint for handling the creation of new ACME accounts.
 */
public class NewAccountEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger instance for logging events.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructs a new instance of the NewAccountEndpoint class.
     *
     * @param provisioner    The provisioner managing this endpoint.
     * @param serverInstance The server instance running this endpoint.
     */
    public NewAccountEndpoint(Provisioner provisioner, ServerInstance serverInstance) {
        super(provisioner, serverInstance);
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
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        // Check nonce
        getServerInstance().getNonceManager().checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        // Deserialize payload and protected objects
        ACMEAccountRequestPayload payload = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMEAccountRequestPayload.class);

        // Check terms of service agreement
        if (!payload.getTermsOfServiceAgreed()) {
            throw new ACMEMalformedException("Terms of Service not accepted. Unable to create account");
        }

        // Validate email addresses
        List<String> emails = payload.getContact();
        if (emails != null) {
            for (String email : emails) {
                email = email.replace("mailto:", "");
                if (!EmailValidation.isValidEmail(email) || email.split("@")[0].equals("localhost")) {
                    LOG.error("E-Mail validation failed for email {}", email);
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

        PublicJsonWebKey publicJsonWebKey;
        try {
            publicJsonWebKey = (PublicJsonWebKey) JsonWebKey.Factory.newJwk(jwkString);
        } catch (JoseException e) {
            LOG.error("Error parsing JWK", e);
            throw new ACMEServerInternalException("Error parsing JWK: " + e.getMessage());
        }

        String publicKeyPEM = PemUtil.convertToPem(publicJsonWebKey.getPublicKey());

        try (Session session = Objects.requireNonNull(getServerInstance().getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();
            ACMEAccount account = new ACMEAccount();
            account.setAccountId(accountId);
            account.setPublicKeyPEM(publicKeyPEM);
            account.setEmails(emails);
            account.setDeactivated(false);
            account.setProvisioner(provisioner.getProvisionerName());
            session.persist(account);
            transaction.commit();
            LOG.info("New ACME account created with account id {}", accountId);
        } catch (Exception e) {
            LOG.error("Unable to create new ACME account", e);
            throw new ACMEServerInternalException(e.getMessage());
        }

        // Construct response
        String nonce = Crypto.createNonce(getServerInstance());
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getAcmeApiURL() + "/acme/acct/" + accountId);
        ctx.header("Replay-Nonce", nonce);
        ctx.status(201); // Created

        AccountResponse response = new AccountResponse();
        response.setStatus(AcmeStatus.VALID.getRfcName());
        response.setContact(emails);
        response.setOrders(provisioner.getAcmeApiURL() + "/acme/acct/" + accountId + "/orders");

        ctx.json(response);
    }
}
