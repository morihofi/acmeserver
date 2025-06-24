package de.morihofi.acmeserver.core.web;

import de.morihofi.acmeserver.core.api.download.DownloadRootCaHandler;
import de.morihofi.acmeserver.server.common.intf.Endpoint;
import de.morihofi.acmeserver.server.common.intf.RoutableHttpServlet;
import de.morihofi.acmeserver.types.httpserver.HandlerType;
import de.morihofi.acmeserver.types.intf.IServerInstance;

/**
 * Servlet exposing root CA downloads.
 */
public class RootCaDownloadServlet extends RoutableHttpServlet {
    public static final String PATH_MOUNT = "/dl/*";
    private final IServerInstance serverInstance;

    public RootCaDownloadServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/dl/rootca/{uuid}.pem", new DownloadRootCaHandler(serverInstance, DownloadRootCaHandler.CertificateFormat.PEM)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/dl/rootca/{uuid}.der", new DownloadRootCaHandler(serverInstance, DownloadRootCaHandler.CertificateFormat.DER)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/dl/rootca/{uuid}.cab", new DownloadRootCaHandler(serverInstance, DownloadRootCaHandler.CertificateFormat.CAB)));
    }
}
