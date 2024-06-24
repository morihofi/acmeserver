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
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.ACMEAccountRequestPayload;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.regex.EmailValidation;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

/**
 * Account Endpoint
 * <p>
 * URL: /acme/account/{id}
 * <p>
 * This endpoint handles the ACME account requests such as updating account information and deactivating accounts.
 */
public class AccountEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger instance for logging account endpoint activities.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Constructs an AccountEndpoint with the given provisioner and server instance.
     *
     * @param provisioner    The provisioner instance.
     * @param serverInstance The server instance.
     */
    public AccountEndpoint(Provisioner provisioner, ServerInstance serverInstance) {
        super(provisioner, serverInstance);
    }

    /**
     * Handles the ACME account request.
     * This method processes the request to update account settings such as email addresses and account status.
     *
     * @param ctx             The context of the HTTP request.
     * @param provisioner     The provisioner instance.
     * @param gson            The Gson instance for JSON processing.
     * @param acmeRequestBody The parsed ACME request body.
     * @throws Exception If an error occurs while handling the request.
     */
    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String accountId = ctx.pathParam("id");

        ACMEAccountRequestPayload acmeAccountRequestPayload = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMEAccountRequestPayload.class);

        performSignatureAndNonceCheck(ctx, accountId, acmeRequestBody);

        // Check if account exists
        ACMEAccount account = ACMEAccount.getAccount(accountId, getServerInstance());
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with ID " + accountId + " not found!");
        }

        try (Session session = Objects.requireNonNull(getServerInstance().getHibernateUtil().getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();

            // Update Account Settings, e.g., Email change
            LOG.info("Update account settings for account {}", account.getAccountId());

            List<String> emails = acmeAccountRequestPayload.getContact();
            if (emails != null) {
                for (String email : emails) {
                    email = email.replace("mailto:", "");

                    if (!EmailValidation.isValidEmail(email) || email.split("\\@")[0].equals("localhost")) {
                        LOG.error("E-Mail format validation failed for email {}", email);
                        throw new ACMEInvalidContactException("E-Mail address format is invalid");
                    }
                    LOG.info("E-Mail validation successful for email {}", email);
                }

                // Update email
                account.getEmails().clear();
                account.getEmails().addAll(emails);
                session.merge(account);
                LOG.info("ACME account {} updated emails to {}", account.getAccountId(), String.join(",", emails));
            }

            String status = acmeAccountRequestPayload.getStatus();
            if (status != null) {
                if (status.equals(AcmeStatus.DEACTIVATED.getRfcName())) {
                    account.setDeactivated(true);
                    session.merge(account);
                    LOG.info("ACME account {} has been deactivated", account.getAccountId());
                }
            }

            transaction.commit();
        }

        ctx.header("Content-Type", "application/json");
        ctx.status(200);
        if (account.isDeactivated()) {
            // Create a new JsonObject
            JsonObject responseObj = new JsonObject();
            // Add the "status" property with the value from AcmeStatus.DEACTIVATED.getRfcName()
            responseObj.addProperty("status", AcmeStatus.DEACTIVATED.getRfcName());
            // Convert the JsonObject to a JSON string
            String jsonResponse = gson.toJson(responseObj);
            // Set the response
            ctx.result(jsonResponse);
        } else {
            ctx.result("{}"); // Empty JSON response
        }
    }
}
