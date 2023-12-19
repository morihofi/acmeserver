package de.morihofi.acmeserver.certificate.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.Identifier;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects.ACMEOrderResponse;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.certificate.acme.security.NonceManager;
import de.morihofi.acmeserver.database.objects.ACMEIdentifier;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import de.morihofi.acmeserver.tools.dateAndTime.DateTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderInfoEndpoint implements Handler {

    private final Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());
    private final Gson gson;

    public OrderInfoEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
        this.gson = new Gson();
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

        ACMERequestBody acmeRequestBody = gson.fromJson(ctx.body(), ACMERequestBody.class);

        // Check signature and nonce
        SignatureCheck.checkSignature(ctx, identifiers.get(0).getOrder().getAccount(), gson);
        NonceManager.checkNonceFromDecodedProtected(acmeRequestBody.getDecodedProtected());


        boolean allVerified = true;
        List<Identifier> identifierList = new ArrayList<>();
        List<String> authorizationsList = new ArrayList<>();

        for (ACMEIdentifier identifier : identifiers) {
            if (!identifier.isVerified()) {
                allVerified = false;
            }
            identifierList.add(new Identifier(identifier.getType(), identifier.getDataValue()));
            authorizationsList.add(provisioner.getApiURL() + "/acme/authz/" + identifier.getAuthorizationId());
        }

        ACMEOrderResponse response = new ACMEOrderResponse();
        response.setExpires(DateTools.formatDateForACME(new Date()));
        response.setStatus(allVerified ? "valid" : "pending");
        response.setFinalize(provisioner.getApiURL() + "/acme/order/" + orderId + "/finalize");
        response.setCertificate(provisioner.getApiURL() + "/acme/order/" + orderId + "/cert");
        response.setIdentifiers(identifierList);

        ctx.result(gson.toJson(response));
    }
}
