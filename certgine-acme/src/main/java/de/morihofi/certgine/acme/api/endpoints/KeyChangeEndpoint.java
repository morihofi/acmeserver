/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.morihofi.certgine.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.certgine.acme.api.objects.ACMERequestBody;
import de.morihofi.certgine.acme.security.SignatureCheck;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.acme.AcmeAccount;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.acme.HttpNonces;
import de.morihofi.certgine.types.exception.exceptions.*;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import java.net.HttpURLConnection;

/**
 * Endpoint for ACME account key rollover.
 */
@Slf4j
public class KeyChangeEndpoint extends AbstractAcmeEndpoint {
    public KeyChangeEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
        JsonObject outerProtected;
        try {
            outerProtected = JsonParser.parseString(acmeRequestBody.getDecodedProtected()).getAsJsonObject();
        } catch (Exception e) {
            throw new ACMEMalformedException("Unable to parse protected header");
        }

        String accountId = SignatureCheck.getAccountIdFromProtectedKID(outerProtected);
        if (accountId == null) {
            throw new ACMEMalformedException("Account id missing in protected header");
        }

        AcmeAccount account = AcmeAccount.getAccount(accountId, getServerInstance());
        if (account == null) {
            throw new ACMEAccountNotFoundException("Account with id " + accountId + " not found");
        }

        performSignatureAndNonceCheck(ctx, account, acmeRequestBody);

        ACMERequestBody innerBody;
        try {
            innerBody = gson.fromJson(acmeRequestBody.getDecodedPayload(), ACMERequestBody.class);
        } catch (Exception e) {
            throw new ACMEMalformedException("Unable to parse inner JWS");
        }

        JsonObject innerPayload;
        try {
            innerPayload = JsonParser.parseString(innerBody.getDecodedPayload()).getAsJsonObject();
        } catch (Exception e) {
            throw new ACMEMalformedException("Unable to parse key-change payload");
        }

        if (!innerPayload.has("account") || !innerPayload.has("oldKey")) {
            throw new ACMEMalformedException("Mandatory fields missing in key-change payload");
        }

        String accountUrl = provisioner.getAcmeApiURL(getServerInstance()) + "/acme/acct/" + accountId;
        if (!accountUrl.equals(innerPayload.get("account").getAsString())) {
            throw new ACMEMalformedException("Account URL mismatch in key-change payload");
        }

        String oldKeyStr = innerPayload.getAsJsonObject("oldKey").toString();
        PublicJsonWebKey oldKey;
        try {
            oldKey = (PublicJsonWebKey) JsonWebKey.Factory.newJwk(oldKeyStr);
        } catch (JoseException | IllegalStateException e) {
            throw new ACMEServerInternalException("Unable to parse old key: " + e.getMessage());
        }
        String oldKeyPem = PemUtil.convertToPem(oldKey.getPublicKey());
        if (!oldKeyPem.equals(account.getPublicKeyPEM())) {
            throw new ACMEUnauthorizedException("Old key does not match account key");
        }

        JsonObject innerProtected;
        try {
            innerProtected = JsonParser.parseString(innerBody.getDecodedProtected()).getAsJsonObject();
        } catch (Exception e) {
            throw new ACMEMalformedException("Unable to parse inner protected header");
        }

        if (!innerProtected.has("jwk")) {
            throw new ACMEMalformedException("Missing JWK for new key");
        }

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
