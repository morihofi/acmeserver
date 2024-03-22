package de.morihofi.acmeserver.certificate.api.serverInfo;

import com.google.gson.Gson;
import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.api.serverInfo.objects.MetadataInfoResponse;
import de.morihofi.acmeserver.certificate.api.serverInfo.objects.ProvisionerResponse;
import de.morihofi.acmeserver.certificate.api.serverInfo.objects.ServerInfoResponse;
import de.morihofi.acmeserver.certificate.api.serverInfo.objects.UpdateResponse;
import de.morihofi.acmeserver.config.ProvisionerConfig;
import de.morihofi.acmeserver.tools.network.scm.github.GitHubVersionChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class ApiServerInfoEndpoint implements Handler {

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(ApiServerInfoEndpoint.class);

    /**
     * List of provisioners, specified in config
     */
    private final List<ProvisionerConfig> provisionerConfigList;

    /**
     * Endpoint for getting server information
     *
     * @param provisionerConfigList List of provisioners, specified in config
     */
    public ApiServerInfoEndpoint(List<ProvisionerConfig> provisionerConfigList) {
        this.provisionerConfigList = provisionerConfigList;
    }

    /**
     * Method for handling the request
     *
     * @param ctx Javalin Context
     */
    @Override
    public void handle(@NotNull Context ctx) {
        ctx.header("Content-Type", "application/json");

        ServerInfoResponse responseData = getServerInfoResponse(provisionerConfigList);

        Gson gson = new Gson();
        String jsonResponse = gson.toJson(responseData);
        ctx.result(jsonResponse);
    }


    private static final Duration CACHE_DURATION = Duration.ofHours(3); // Cache for 3 hours
    private static String cachedLatestReleaseTag = null;
    private static String cachedLatestReleaseUrl = null;
    private static Instant cacheTimestamp = Instant.MIN;

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

        {
            String latestReleaseUrl = null;
            boolean isUpdateAvailable = false;

            // Check whether the cache is still valid
            if (Duration.between(cacheTimestamp, Instant.now()).compareTo(CACHE_DURATION) > 0) {
                // Cache has expired, so retrieve data again
                try {
                    cachedLatestReleaseTag = GitHubVersionChecker.getLatestReleaseTag();
                    cachedLatestReleaseUrl = GitHubVersionChecker.getLatestReleaseURL();
                    cacheTimestamp = Instant.now();
                } catch (IOException ex) {
                    log.error("Failed to fetch the latest release information");
                }
            }

            // Use the cached data
            if (Main.buildMetadataGitClosestTagName != null && !Main.buildMetadataGitClosestTagName.equalsIgnoreCase(cachedLatestReleaseTag)) {
                latestReleaseUrl = cachedLatestReleaseUrl;
                isUpdateAvailable = true;
            }

            metadataInfo.setUpdate(new UpdateResponse(isUpdateAvailable, latestReleaseUrl));
        }


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
