package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.NonceManager;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.Base64Tools;
import de.morihofi.acmeserver.tools.Crypto;
import de.morihofi.acmeserver.tools.RegexTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

public class NewAccountEndpoint implements Handler {
    public final Logger log = LogManager.getLogger(getClass());
    private Provisioner provisioner;

    public NewAccountEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        //Parse request body
        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        //Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());


        //Payload is Base64 Encoded
        JSONObject reqBodyPayloadObj = new JSONObject(acmeRequestBody.getDecodedPayload());
        JSONObject reqBodyProtectedObj = new JSONObject(acmeRequestBody.getDecodedProtected());

        boolean reqPayloadTermsOfServiceAgreed = reqBodyPayloadObj.optBoolean("termsOfServiceAgreed");

        if (!reqPayloadTermsOfServiceAgreed) {
            throw new ACMEMalformedException("Terms of Service not accepted. Unable to create account");
        }

        //String reqPayloadContactEmail = "";
        ArrayList<String> emailsFromPayload = new ArrayList<>();
        // Has email? (This can be updated later)
        if (reqBodyPayloadObj.has("contact")) {


            for (int i = 0; i < reqBodyPayloadObj.getJSONArray("contact").length(); i++) {
                String email = reqBodyPayloadObj.getJSONArray("contact").getString(i);
                email = email.replace("mailto:", "");

                if (!RegexTools.isValidEmail(email) || email.split("\\@")[0].equals("localhost")) {
                    log.error("E-Mail validation failed for email \"" + email + "\"");
                    throw new ACMEInvalidContactException("Mail validation failed for email " + email);
                }
                log.info("E-Mail validation successful for email \"" + email + "\"");
                emailsFromPayload.add(email);
            }
        }


        // Create new account in database
        // https://ietf-wg-acme.github.io/acme/draft-ietf-acme-acme.html#rfc.section.7.3

        String accountId = UUID.randomUUID().toString();
        Database.createAccount(accountId, reqBodyProtectedObj.getJSONObject("jwk").toString(), emailsFromPayload);

        String nonce = Crypto.createNonce();
        // Response is JSON
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getApiURL() + "/acme/acct/" + accountId);
        ctx.header("Replay-Nonce", nonce);
        ctx.status(201); //Created

        JSONObject responseJSON = new JSONObject();


        //Contact information
        JSONArray contactEmailsArr = new JSONArray();
        for (String email : emailsFromPayload) {
            contactEmailsArr.put(email);
        }

        responseJSON.put("status", "valid");
        if (contactEmailsArr.length() != 0) {
            responseJSON.put("contact", contactEmailsArr);
        }

        responseJSON.put("orders", provisioner.getApiURL() + "/acme/acct/" + accountId + "/orders");

        ctx.result(responseJSON.toString());

    }
}
