package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.tools.RegexTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * New Order Endpoint
 * <p>
 * URL: /acme/new-order
 */
public class AccountEndpoint implements Handler {
    private Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public AccountEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String accountId = ctx.pathParam("id");

        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);
        //Check signature
        SignatureCheck.checkSignature(ctx, accountId, gson);

        //Check if account exists
        ACMEAccount account = Database.getAccount(accountId);
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with ID " + accountId + " not found!");
        }
        JSONObject reqBodyPayloadObj = new JSONObject(acmeRequestBody.getDecodedPayload());

        // Update Account Settings, e.g. E-Mail change
        log.info("Update account settings for account \"" + accountId + "\"");


        ArrayList<String> emailsFromPayload = new ArrayList<>();
        // Has email? (This can be updated later)
        if (reqBodyPayloadObj.has("contact")) {
            for (int i = 0; i < reqBodyPayloadObj.getJSONArray("contact").length(); i++) {
                String email = reqBodyPayloadObj.getJSONArray("contact").getString(i);
                email = email.replace("mailto:", "");

                if (!RegexTools.isValidEmail(email) || email.split("\\@")[0].equals("localhost")) {
                    log.error("E-Mail validation failed for email \"" + email + "\"");
                    throw new ACMEInvalidContactException("E-Mail address is invalid");
                }
                log.info("E-Mail validation successful for email \"" + email + "\"");
                emailsFromPayload.add(email);
            }

            //reqPayloadContactEmail = reqBodyPayloadObj.getJSONArray("contact").getString(0);
            //reqPayloadContactEmail = reqPayloadContactEmail.replace("mailto:", "");

        }

        //Update Contact Emails
        Database.updateAccountEmail(account, emailsFromPayload);


        JSONObject responseJSON = new JSONObject();
        ctx.header("Content-Type", "application/jose+json");
        ctx.status(200);
        ctx.result(responseJSON.toString());
    }
}
