package de.morihofi.certgine.core.servlet.download;

import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;

/**
 * Servlet exposing root CA downloads.
 */
@ServletMount(servletMountPoint = "/dl/*", protect = true)
public class RootCaDownloadServlet extends RoutableHttpServlet {
    private final IServerInstance serverInstance;

    public RootCaDownloadServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/dl/rootca/{uuid}.pem", new DownloadRootCaHandler(serverInstance, DownloadRootCaHandler.CertificateFormat.PEM)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/dl/rootca/{uuid}.der", new DownloadRootCaHandler(serverInstance, DownloadRootCaHandler.CertificateFormat.DER)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/dl/rootca/{uuid}.cab", new DownloadRootCaHandler(serverInstance, DownloadRootCaHandler.CertificateFormat.CAB)));
    }
}
