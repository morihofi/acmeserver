package de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.ServerInfo;

import com.google.gson.Gson;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.ServerInfo.objects.MetadataInfoResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.ServerInfo.objects.ProvisionerResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.ServerInfo.objects.ServerInfoResponse;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ServerInfoEndpoint implements Handler {

    private final List<ProvisionerConfig> provisionerConfigList;

    public ServerInfoEndpoint(List<ProvisionerConfig> provisionerConfigList) {
        this.provisionerConfigList = provisionerConfigList;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/json");

        MetadataInfoResponse metadataInfo = new MetadataInfoResponse();
        metadataInfo.setVersion(Main.buildMetadataVersion);
        metadataInfo.setBuildTime(Main.buildMetadataBuildTime);
        metadataInfo.setGitCommit(Main.buildMetadataGitCommit);
        metadataInfo.setJavaVersion(System.getProperty("java.version"));
        metadataInfo.setOperatingSystem(System.getProperty("os.name"));

        List<ProvisionerResponse> provisioners = new ArrayList<>();
        for (ProvisionerConfig provisionerConfig : provisionerConfigList) {
            ProvisionerResponse provisioner = new ProvisionerResponse();
            provisioner.setName(provisionerConfig.getName());
            provisioners.add(provisioner);
        }

        ServerInfoResponse responseData = new ServerInfoResponse();
        responseData.setMetadataInfo(metadataInfo);
        responseData.setProvisioners(provisioners);

        Gson gson = new Gson();
        String jsonResponse = gson.toJson(responseData);
        ctx.result(jsonResponse);
    }
}
