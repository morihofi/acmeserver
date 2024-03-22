package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;


public class DirectoryEndpoint implements Handler {
    private final Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DirectoryEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Method for handling the request
     * @param ctx Javalin Context
     * @throws Exception thrown when there was an error processing the request
     */
    @Override
    public void handle(@NotNull Context ctx) throws Exception {

        // Response is JSON
        ctx.header("Content-Type", "application/json");

        JSONObject responseJSON = new JSONObject();

        JSONObject metaObject = new JSONObject();
        metaObject.put("website", provisioner.getAcmeMetadataConfig().getWebsite().trim());
        metaObject.put("termsOfService", provisioner.getAcmeMetadataConfig().getTos().trim());


        responseJSON.put("meta", metaObject);
        responseJSON.put("newAccount", provisioner.getApiURL() + "/acme/new-acct");
        responseJSON.put("newNonce", provisioner.getApiURL() + "/acme/new-nonce");
        responseJSON.put("newOrder", provisioner.getApiURL() + "/acme/new-order");
        responseJSON.put("revokeCert", provisioner.getApiURL() + "/acme/revoke-cert");
        responseJSON.put("keyChange", provisioner.getApiURL() + "/acme/key-change");

        ctx.result(responseJSON.toString());

    }
}
