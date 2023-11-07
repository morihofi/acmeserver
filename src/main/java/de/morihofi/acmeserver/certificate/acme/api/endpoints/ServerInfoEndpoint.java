package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import de.morihofi.acmeserver.Main;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class ServerInfoEndpoint implements Handler {
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/json");

        JSONObject returnObj = new JSONObject();
        returnObj.put("version", Main.buildMetadataVersion);
        returnObj.put("buildtime", Main.buildMetadataBuildTime);
        returnObj.put("gitcommit", Main.buildMetadataGitCommit);
        returnObj.put("javaversion", System.getProperty("java.version"));
        returnObj.put("os", System.getProperty("os.name"));

        ctx.result(returnObj.toString());
    }
}
