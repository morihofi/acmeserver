package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;


public class DirectoryEndpoint implements Handler {
    private final Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public DirectoryEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Handles an HTTP request by generating and returning a JSON response containing ACME (Automated Certificate Management
     * Environment) metadata and endpoint URLs for various ACME operations.
     *
     * @param ctx The Context object representing the HTTP request and response.
     * @throws Exception if there is an issue with handling the HTTP request.
     */
    @Override
    public void handle(@NotNull Context ctx) throws Exception {

        // Response is JSON
        ctx.header("Content-Type", "application/json");

        JSONObject responseJSON = new JSONObject();

        JSONObject metaObject = new JSONObject();
        metaObject.put("website", provisioner.getAcmeMetadataConfig().getWebsite());
        metaObject.put("termsOfService", provisioner.getAcmeMetadataConfig().getTos());


        responseJSON.put("meta", metaObject);
        responseJSON.put("newAccount", provisioner.getApiURL() + "/acme/new-acct");
        responseJSON.put("newNonce", provisioner.getApiURL() + "/acme/new-nonce");
        responseJSON.put("newOrder", provisioner.getApiURL() + "/acme/new-order");
        responseJSON.put("revokeCert", provisioner.getApiURL() + "/acme/revoke-cert");
        responseJSON.put("keyChange", provisioner.getApiURL() + "/acme/key-change");

        ctx.result(responseJSON.toString());

    }
}
