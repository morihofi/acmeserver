package de.morihofi.acmeserver.core.api.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.acmeserver.core.api.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.core.api.acme.api.objects.ACMERequestBody;
import de.morihofi.acmeserver.core.api.acme.security.SignatureCheck;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.types.database.entities.AcmeAccount;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.HttpNonces;
import de.morihofi.acmeserver.types.exception.exceptions.*;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import java.net.HttpURLConnection;

/**
 * Endpoint for ACME account key rollover.
 * <p>
 * <b>This is an experimental feature implementation which has not been fully tested yet. Use with caution.</b>
 */
@Slf4j
public class KeyChangeEndpoint extends AbstractAcmeEndpoint {
    public KeyChangeEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

    @Override
    public void handleRequest(@NotNull Context ctx, @NotNull AcmeProvisioner provisioner, @NotNull Gson gson, @NotNull ACMERequestBody acmeRequestBody) throws Exception {
        String accountId = SignatureCheck.getAccountIdFromProtectedKID(acmeRequestBody.getDecodedProtected());
        if (accountId == null) {
            throw new ACMEMalformedException("Account id missing in protected header");
        }

        AcmeAccount account = AcmeAccount.getAccount(accountId, getServerInstance());
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with id " + accountId + " not found");
        }

        performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

        ACMERequestBody innerBody = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMERequestBody.class);
        JsonObject innerPayload = JsonParser.parseString(innerBody.getDecodedPayload()).getAsJsonObject();

        String accountUrl = provisioner.getAcmeApiURL(getServerInstance()) + "/acme/acct/" + accountId;
        if (!accountUrl.equals(innerPayload.get("account").getAsString())) {
            throw new ACMEMalformedException("Account URL mismatch in key-change payload");
        }

        String oldKeyStr = innerPayload.getAsJsonObject("oldKey").toString();
        PublicJsonWebKey oldKey;
        try {
            oldKey = (PublicJsonWebKey) JsonWebKey.Factory.newJwk(oldKeyStr);
        } catch (JoseException e) {
            throw new ACMEServerInternalException("Unable to parse old key: " + e.getMessage());
        }
        String oldKeyPem = PemUtil.convertToPem(oldKey.getPublicKey());
        if (!oldKeyPem.equals(account.getPublicKeyPEM())) {
            throw new ACMEUnauthorizedException("Old key does not match account key");
        }

        JsonObject innerProtected = JsonParser.parseString(innerBody.getDecodedProtected()).getAsJsonObject();
        String newKeyStr = innerProtected.getAsJsonObject("jwk").toString();
        PublicJsonWebKey newKey;
        try {
            newKey = (PublicJsonWebKey) JsonWebKey.Factory.newJwk(newKeyStr);
        } catch (JoseException e) {
            throw new ACMEServerInternalException("Unable to parse new key: " + e.getMessage());
        }

        String newKeyPem = PemUtil.convertToPem(newKey.getPublicKey());

        try (Session session = getServerInstance().getDatabaseSession()) {
            Transaction tx = session.beginTransaction();
            account.setPublicKeyPEM(newKeyPem);
            session.merge(account);
            tx.commit();
        }

        ctx.status(HttpURLConnection.HTTP_OK);
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));
        ctx.header("Content-Type", "application/json");
        ctx.result("{}");
    }
}
