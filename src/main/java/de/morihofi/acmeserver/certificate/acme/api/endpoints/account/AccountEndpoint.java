package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.ACMEAccountRequestPayload;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.tools.regex.EmailValidation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Objects;

/**
 * New Order Endpoint
 * <p>
 * URL: /acme/new-order
 */
public class AccountEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    public final Logger log = LogManager.getLogger(getClass());

    /**
     * Endpoint for managing ACME Account settings. Change E-Mail etc.
     *
     * @param provisioner Provisioner instance
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AccountEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        String accountId = ctx.pathParam("id");

        ACMEAccountRequestPayload acmeAccountRequestPayload = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMEAccountRequestPayload.class);

        performSignatureAndNonceCheck(ctx, accountId, acmeRequestBody);

        // Check if account exists
        ACMEAccount account = ACMEAccount.getAccount(accountId);
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with ID " + accountId + " not found!");
        }


        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
            Transaction transaction = session.beginTransaction();


            // Update Account Settings, e.g., Email change
            log.info("Update account settings for account {}", account.getAccountId());

            List<String> emails = acmeAccountRequestPayload.getContact();
            if (emails != null) {
                for (String email : emails) {
                    email = email.replace("mailto:", "");

                    if (!EmailValidation.isValidEmail(email) || email.split("\\@")[0].equals("localhost")) {
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
        }

        ctx.header("Content-Type", "application/json");
        ctx.status(200);
        if(account.isDeactivated()){
            // Create a new JsonObject
            JsonObject responseObj = new JsonObject();
            // Add the "status" property with the value from AcmeStatus.DEACTIVATED.getRfcName()
            responseObj.addProperty("status", AcmeStatus.DEACTIVATED.getRfcName());
            // Create a Gson instance

            // Convert the JsonObject to a JSON string
            String jsonResponse = gson.toJson(responseObj);

            ctx.result(jsonResponse);
        }else {
            ctx.result("{}"); // Empty JSON response
        }
    }


}
