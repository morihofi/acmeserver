/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.account;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.morihofi.certgine.acme.servlets.handlerapi.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.account.objects.ACMEAccountRequestPayload;
import de.morihofi.certgine.acme.servlets.handlerapi.objects.ACMERequestBody;
import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.acme.types.events.AcmeAccountDeactivatedEvent;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.certgine.types.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.utils.regex.EmailValidator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Account Endpoint
 * <p>
 * URL: /acme/account/{id}
 * <p>
 * This endpoint handles the ACME account requests such as updating account information and deactivating accounts.
 */
@Slf4j
public class AccountEndpoint extends AbstractAcmeEndpoint {

    /**
     * Constructs an AccountEndpoint with the given provisioner and server instance.
     *
     * @param serverInstance The server instance.
     */
    public AccountEndpoint(CertgineModuleInstance moduleInstance) {
        super(moduleInstance);
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
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
        String accountId = ctx.pathParam("id");

        ACMEAccountRequestPayload acmeAccountRequestPayload = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMEAccountRequestPayload.class);

        performSignatureAndNonceCheck(ctx, accountId, acmeRequestBody);

        // Check if account exists
        AcmeAccount account = AcmeAccount.getAccount(accountId, getModuleInstance().getModule().getServerInstance());
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with ID " + accountId + " not found!");
        }

        try (Session session = getModuleInstance().getModule().getServerInstance().getDatabaseSession()) {
            Transaction transaction = session.beginTransaction();

            // Update Account Settings, e.g., Email change
            log.info("Update account settings for account {}", account.getAccountId());

            List<String> emails = acmeAccountRequestPayload.getContact();
            if (emails != null) {
                for (String email : emails) {
                    email = email.replace("mailto:", "");

                    if (!EmailValidator.isValidEmail(email) || email.split("\\@")[0].equals("localhost")) {
                        log.error("E-Mail format validation failed for email {}", email);
                        throw new ACMEInvalidContactException("E-Mail address format is invalid");
                    }
                    log.info("E-Mail validation successful for email {}", email);
                }

                // Update email
                account.getEmails().clear();
                account.getEmails().addAll(emails);
                session.merge(account);
                log.info("ACME account {} updated emails to {}", account.getAccountId(), String.join(",", emails));
            }

            String status = acmeAccountRequestPayload.getStatus();
            if (status != null) {
                if (status.equals(AcmeStatus.DEACTIVATED.getRfcName())) {
                    account.setDeactivated(true);
                    session.merge(account);
                    log.info("ACME account {} has been deactivated", account.getAccountId());
                }
            }

            transaction.commit();
            if (account.isDeactivated()) {
                getModuleInstance().getModule().getServerInstance().getEventBus().publish(new AcmeAccountDeactivatedEvent(account));
            }
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
