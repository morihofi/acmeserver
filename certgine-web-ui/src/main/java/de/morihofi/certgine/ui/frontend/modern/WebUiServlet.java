package de.morihofi.certgine.ui.frontend.modern;

import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServletMount(servletMountPoint = "/*", protect = true)
public class WebUiServlet extends RoutableHttpServlet {
    public WebUiServlet(IServerInstance si) {
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/", context -> {
            context.result("Certgine is running!");
        }));
    }
}