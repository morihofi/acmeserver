package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import com.google.gson.Gson;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.security.SignatureCheck;
import de.morihofi.acmeserver.certificate.objects.ACMERequestBody;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.exception.exceptions.ACMEAccountNotFoundException;
import de.morihofi.acmeserver.exception.exceptions.ACMEInvalidContactException;
import de.morihofi.acmeserver.tools.RegexTools;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;


public class DirectoryEndpoint implements Handler {
    private Provisioner provisioner;
    public final Logger log = LogManager.getLogger(getClass());

    public DirectoryEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

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
