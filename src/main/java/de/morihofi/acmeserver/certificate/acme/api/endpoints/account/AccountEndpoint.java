package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.ACMEAccountRequestPayload;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.tools.regex.EmailValidation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

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
        ACMEAccount account = Database.getAccount(accountId);
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with ID " + accountId + " not found!");
        }

        // Update Account Settings, e.g., Email change
        log.info("Update account settings for account {}", accountId);

        List<String> emails = acmeAccountRequestPayload.getContact();
        if (emails != null) {
            for (String email : emails) {
                email = email.replace("mailto:", "");

                if (!EmailValidation.isValidEmail(email) || email.split("\\@")[0].equals("localhost")) {
                    log.error("E-Mail validation failed for email {}", email);
                    throw new ACMEInvalidContactException("E-Mail address is invalid");
                }
                log.info("E-Mail validation successful for email {}", email);
            }

            // Update Contact Emails in Database
            Database.updateAccountEmail(account, emails);
        }

        ctx.header("Content-Type", "application/json");
        ctx.status(200);
        ctx.result("{}"); // Empty JSON response
    }


}
