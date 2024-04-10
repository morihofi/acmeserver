package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;


public class DirectoryEndpoint implements Handler {
    private final Provisioner provisioner;
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DirectoryEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Method for handling the request
     * @param ctx Javalin Context
     */
    @Override
    public void handle(@NotNull Context ctx) {

        // Response is JSON
        ctx.header("Content-Type", "application/json");

        // Create the Gson instance
        Gson gson = new Gson();

        // Create the meta object
        JsonObject metaObject = new JsonObject();
        metaObject.addProperty("website", provisioner.getAcmeMetadataConfig().getWebsite().trim());
        metaObject.addProperty("termsOfService", provisioner.getAcmeMetadataConfig().getTos().trim());

        // Create the main JSON object
        JsonObject responseJSON = new JsonObject();
        responseJSON.add("meta", metaObject);
        responseJSON.addProperty("newAccount", provisioner.getApiURL() + "/acme/new-acct");
        responseJSON.addProperty("newNonce", provisioner.getApiURL() + "/acme/new-nonce");
        responseJSON.addProperty("newOrder", provisioner.getApiURL() + "/acme/new-order");
        responseJSON.addProperty("revokeCert", provisioner.getApiURL() + "/acme/revoke-cert");
        responseJSON.addProperty("keyChange", provisioner.getApiURL() + "/acme/key-change");

        // Convert the JsonObject to a String
        String jsonResponse = gson.toJson(responseJSON);

        ctx.result(jsonResponse);

    }
}
