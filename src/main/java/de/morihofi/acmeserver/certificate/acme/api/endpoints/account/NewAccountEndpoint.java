package de.morihofi.acmeserver.certificate.acme.api.endpoints.account;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.certificate.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.ACMEAccountRequestPayload;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects.AccountResponse;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.exception.exceptions.ACMEMalformedException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
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

public class NewAccountEndpoint extends AbstractAcmeEndpoint {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    public NewAccountEndpoint(Provisioner provisioner) {
        super(provisioner);
    }

    @Override
    public void handleRequest(Context ctx, Provisioner provisioner, Gson gson, ACMERequestBody acmeRequestBody) throws Exception {
        // Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

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

        try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {
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
        String nonce = Crypto.createNonce();
        ctx.header("Content-Type", "application/json");
        ctx.header("Location", provisioner.getApiURL() + "/acme/acct/" + accountId);
        ctx.header("Replay-Nonce", nonce);
        ctx.status(201); // Created

        AccountResponse response = new AccountResponse();
        response.setStatus(AcmeStatus.VALID.getRfcName());
        response.setContact(emails);
        response.setOrders(provisioner.getApiURL() + "/acme/acct/" + accountId + "/orders");

        ctx.json(response);
    }
}
