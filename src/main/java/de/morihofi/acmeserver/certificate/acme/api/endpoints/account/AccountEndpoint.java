package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.ACMEAccountRequestBody;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.tools.regex.EmailValidation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * New Order Endpoint
 * <p>
 * URL: /acme/new-order
 */
public class AccountEndpoint implements Handler {
    private final Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());
    private final Gson gson;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AccountEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
        this.gson = new Gson();
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String accountId = ctx.pathParam("id");

        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);
        ACMEAccountRequestBody acmeAccountRequestBody = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMEAccountRequestBody.class);

        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, accountId, gson);
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        // Check if account exists
        ACMEAccount account = Database.getAccount(accountId);
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with ID " + accountId + " not found!");
        }

        // Update Account Settings, e.g., Email change
        log.info("Update account settings for account {}", accountId);

        List<String> emails = acmeAccountRequestBody.getContact();
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

        ctx.header("Content-Type", "application/jose+json");
        ctx.status(200);
        ctx.result("{}"); // Empty JSON response
    }

}
