/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.api.mgmt.serverInfo;

import de.morihofi.acmeserver.core.Main;
import de.morihofi.acmeserver.core.api.mgmt.serverInfo.objects.MetadataInfoResponse;
import de.morihofi.acmeserver.core.api.mgmt.serverInfo.objects.ProvisionerResponse;
import de.morihofi.acmeserver.core.api.mgmt.serverInfo.objects.ServerInfoResponse;
import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.NonNull;


import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoint for handling server information requests.
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class ApiServerInfoEndpoint implements Handler {

    /**
     * List of provisioners, specified in config.
     */
    private final IServerInstance serverInstance;

    /**
     * Constructs a new endpoint for retrieving server information.
     *
     * @param serverInstance Instance of the Server.
     */
    public ApiServerInfoEndpoint(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Retrieves the server information response.
     *
     * @return ServerInfoResponse containing server metadata and provisioner information.
     */
    public ServerInfoResponse getServerInfoResponse() {
        MetadataInfoResponse metadataInfo = new MetadataInfoResponse();
        metadataInfo.setVersion(serverInstance.getBuildMetadata().getBuildVersion());
        metadataInfo.setBuildTime(serverInstance.getBuildMetadata().getBuildTime());
        metadataInfo.setGitCommit(serverInstance.getBuildMetadata().getGitCommit());
        metadataInfo.setJavaVersion(System.getProperty("java.version"));
        metadataInfo.setOperatingSystem(System.getProperty("os.name"));
        metadataInfo.setJvmUptime(ManagementFactory.getRuntimeMXBean().getUptime() / 1000L);
        metadataInfo.setJvmStartTime(ManagementFactory.getRuntimeMXBean().getStartTime() / 1000L);
        metadataInfo.setStartupTime(Main.startupTime); // already in seconds
        metadataInfo.setHost(serverInstance.getAppConfig().getServer().getDnsName());
        metadataInfo.setHttpsPort(serverInstance.getAppConfig().getServer().getPorts().getHttps());

        List<ProvisionerResponse> provisioners = new ArrayList<>();
        for (AcmeProvisioner provisionerConfig : AcmeProvisioner.getAllProvisioners(serverInstance)) {
            ProvisionerResponse provisioner = new ProvisionerResponse();
            provisioner.setName(provisionerConfig.getName());
            provisioners.add(provisioner);
        }

        ServerInfoResponse responseData = new ServerInfoResponse();
        responseData.setMetadataInfo(metadataInfo);
        responseData.setProvisioners(provisioners);

        return responseData;
    }

    /**
     * Handles the request to retrieve server information.
     *
     * @param ctx The Javalin context.
     */
    @Override
    public void handle(@NonNull HandlerContext ctx) {
        ctx.header("Content-Type", "application/json");

        ServerInfoResponse responseData = getServerInfoResponse();
        ctx.json(responseData);
    }
}
