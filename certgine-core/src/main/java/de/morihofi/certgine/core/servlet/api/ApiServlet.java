package de.morihofi.certgine.core.servlet.api;

import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;

@ServletMount(servletMountPoint = "/api/*", protect = true)
public class ApiServlet extends RoutableHttpServlet {
    private final IServerInstance serverInstance;

    public ApiServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        // getRouter().addHandler(new Endpoint(HandlerType.POST, "/api/graphql", new GraphQLEndpoint(null /* TODO: Fixme */)));
    }
}
