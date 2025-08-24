package de.morihofi.certgine.clientinstallagent.servlets;

import de.morihofi.certgine.clientinstallagent.ClientInstallAgentModule;
import de.morihofi.certgine.clientinstallagent.ClientInstallAgentModuleInstance;
import de.morihofi.certgine.clientinstallagent.servlets.endpoints.DownloadEndpoint;
import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServletMount(servletMountPoint = "/native-agent/*")

public class ClientInstallAgentServlet extends RoutableHttpServlet {
    public ClientInstallAgentServlet(CertgineModuleInstance cgModuleInstance) {
        ClientInstallAgentModuleInstance moduleInstance = (ClientInstallAgentModuleInstance) cgModuleInstance;

        // ACME Directory
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/native-agent/dl", new DownloadEndpoint(moduleInstance)));
    }
}
