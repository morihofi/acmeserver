package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class ServerInfoEndpoint implements Handler {

    private final List<ProvisionerConfig> provisionerConfigList;

    public ServerInfoEndpoint(List<ProvisionerConfig> provisionerConfigList) {
        this.provisionerConfigList = provisionerConfigList;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/json");

        JSONObject returnObj = new JSONObject();
        returnObj.put("version", Main.buildMetadataVersion);
        returnObj.put("buildtime", Main.buildMetadataBuildTime);
        returnObj.put("gitcommit", Main.buildMetadataGitCommit);
        returnObj.put("javaversion", System.getProperty("java.version"));
        returnObj.put("os", System.getProperty("os.name"));

        JSONArray provisionersArr = new JSONArray();

        for (ProvisionerConfig provisionerConfig : provisionerConfigList){
            JSONObject activeProvisioner = new JSONObject();
            activeProvisioner.put("name",provisionerConfig.getName());

            provisionersArr.put(activeProvisioner);
        }

        returnObj.put("provisioners", provisionersArr);

        ctx.result(returnObj.toString());
    }
}
