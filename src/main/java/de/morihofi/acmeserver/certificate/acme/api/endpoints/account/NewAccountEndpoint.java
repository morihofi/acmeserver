package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.ACMEAccountRequestBody;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.AccountResponse;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.regex.EmailValidation;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class NewAccountEndpoint implements Handler {
    public final Logger log = LogManager.getLogger(getClass());
    private final Provisioner provisioner;
    private final Gson gson;

    public NewAccountEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
        this.gson = new Gson();
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        // Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        // Deserialize payload and protected objects
        ACMEAccountRequestBody payload = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMEAccountRequestBody.class);
        JSONObject reqBodyProtectedObj = new JSONObject(acmeRequestBody.getDecodedProtected());

        // Check terms of service agreement
        if (payload.getTermsOfServiceAgreed() == null || !payload.getTermsOfServiceAgreed()) {
            throw new ACMEMalformedException("Terms of Service not accepted. Unable to create account");
        }

        // Validate email addresses
        List<String> emails = payload.getContact();
        if (emails != null) {
            for (String email : emails) {
                email = email.replace("mailto:", "");
                if (!EmailValidation.isValidEmail(email) || email.split("@")[0].equals("localhost")) {
                    log.error("E-Mail validation failed for email {}", email);
                    throw new ACMEInvalidContactException("Mail validation failed for email " + email);
                }
            }
        }

        // Create new account in database
        String accountId = UUID.randomUUID().toString();
        Database.createAccount(accountId, reqBodyProtectedObj.getJSONObject("jwk").toString(), emails);

        // Construct response
        String nonce = Crypto.createNonce();
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getApiURL() + "/acme/acct/" + accountId);
        ctx.header("Replay-Nonce", nonce);
        ctx.status(201); // Created

        AccountResponse response = new AccountResponse();
        response.setStatus("valid");
        response.setContact(emails);
        response.setOrders(provisioner.getApiURL() + "/acme/acct/" + accountId + "/orders");

        ctx.result(gson.toJson(response));
    }

}
