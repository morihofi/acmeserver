package de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo;

import com.google.gson.Gson;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.MetadataInfoResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ProvisionerResponse;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class ServerInfoEndpoint implements Handler {

    /**
     * List of provisioners, specified in config
     */
    private final List<ProvisionerConfig> provisionerConfigList;

    /**
     * Endpoint for getting server information
     * @param provisionerConfigList List of provisioners, specified in config
     */
    public ServerInfoEndpoint(List<ProvisionerConfig> provisionerConfigList) {
        this.provisionerConfigList = provisionerConfigList;
    }

    /**
     * Method for handling the request
     * @param ctx Javalin Context
     * @throws Exception thrown when there was an error processing the request
     */
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/json");

        ServerInfoResponse responseData = getServerInfoResponse(provisionerConfigList);

        Gson gson = new Gson();
        String jsonResponse = gson.toJson(responseData);
        ctx.result(jsonResponse);
    }

    public static ServerInfoResponse getServerInfoResponse(List<ProvisionerConfig> provisionerConfigList) {
        MetadataInfoResponse metadataInfo = new MetadataInfoResponse();
        metadataInfo.setVersion(Main.buildMetadataVersion);
        metadataInfo.setBuildTime(Main.buildMetadataBuildTime);
        metadataInfo.setGitCommit(Main.buildMetadataGitCommit);
        metadataInfo.setJavaVersion(System.getProperty("java.version"));
        metadataInfo.setOperatingSystem(System.getProperty("os.name"));
        metadataInfo.setJvmUptime(ManagementFactory.getRuntimeMXBean().getUptime() / 1000L);
        metadataInfo.setJvmStartTime(ManagementFactory.getRuntimeMXBean().getStartTime() / 1000L);
        metadataInfo.setStartupTime(Main.startupTime); //already in seconds
        metadataInfo.setHost(Main.appConfig.getServer().getDnsName());
        metadataInfo.setHttpsPort(Main.appConfig.getServer().getPorts().getHttps());

        List<ProvisionerResponse> provisioners = new ArrayList<>();
        for (ProvisionerConfig provisionerConfig : provisionerConfigList) {
            ProvisionerResponse provisioner = new ProvisionerResponse();
            provisioner.setName(provisionerConfig.getName());
            provisioners.add(provisioner);
        }

        ServerInfoResponse responseData = new ServerInfoResponse();
        responseData.setMetadataInfo(metadataInfo);
        responseData.setProvisioners(provisioners);

        return responseData;
    }

}
