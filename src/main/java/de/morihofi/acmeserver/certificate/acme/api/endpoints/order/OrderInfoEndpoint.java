package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.tools.Crypto;
import de.morihofi.acmeserver.tools.DateTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class OrderInfoEndpoint implements Handler {

    private Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public OrderInfoEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String orderId = ctx.pathParam("orderId");

        ctx.header("Content-Type", "application/json");
        ctx.header("Replay-Nonce", Crypto.createNonce());

        List<ACMEIdentifier> identifiers = Database.getACMEIdentifiersByOrderId(orderId);
        if (identifiers.isEmpty()) {
            throw new IllegalArgumentException("Identifiers empty, FIXME");
        }

        Gson gson = new Gson();
        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        //Check signature
        SignatureCheck.checkSignature(ctx, identifiers.get(0).getOrder().getAccount(), gson);
        //Check nonce
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());

        JSONObject responseObj = new JSONObject();


        // TODO: Valid Expires Date
        responseObj.put("expires", DateTools.formatDateForACME(new Date()));


        JSONArray identifiersArr = new JSONArray();
        JSONArray authorizationsArr = new JSONArray();

        boolean allVerified = true;
        for (ACMEIdentifier identifier : Database.getACMEIdentifiersByOrderId(orderId)) {

            if (!identifier.isVerified()) {
                allVerified = false;
            }

            JSONObject identifierObj = new JSONObject();
            identifierObj.put("type", identifier.getType());
            identifierObj.put("value", identifier.getDataValue());

            authorizationsArr.put(provisioner.getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());

        }


        responseObj.put("identifiers", identifiersArr);
        if (allVerified) {
            responseObj.put("status", "valid");
        } else {
            responseObj.put("status", "pending");
        }


        responseObj.put("finalize", provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");
        // Get certificate for order
        //responseObj.put("certificate", getApiURL() + "/acme/cert/" + "fixme");
        responseObj.put("certificate", provisioner.getApiURL() + "/acme/order/" + orderId + "/cert");


        ctx.result(responseObj.toString());
    }
}
