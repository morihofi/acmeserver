package de.morihofi.acmeserver.revocation;

import de.morihofi.acmeserver.revocation.endpoints.CRLEndpoint;
import de.morihofi.acmeserver.revocation.endpoints.OcspEndpointGet;
import de.morihofi.acmeserver.revocation.endpoints.OcspEndpointPost;
import de.morihofi.acmeserver.server.common.intf.Endpoint;
import de.morihofi.acmeserver.server.common.intf.RoutableHttpServlet;
import de.morihofi.acmeserver.types.httpserver.HandlerType;
import de.morihofi.acmeserver.types.intf.IServerInstance;

/**
 * Servlet serving certificate revocation related endpoints like CRL and OCSP.
 */
public class RevocationHttpServlet extends RoutableHttpServlet {
    /**
     * Path mount for this servlet.
     */
    public static final String PATH_MOUNT = "/revocation/*";
    private final IServerInstance serverInstance;

    public RevocationHttpServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/revocation/{provisioner}/crl/certs-revoked.crl", new CRLEndpoint(serverInstance)));
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/revocation/{provisioner}/ocsp", new OcspEndpointPost(serverInstance)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/revocation/{provisioner}/ocsp/{ocspRequest}", new OcspEndpointGet(serverInstance)));
    }
}
