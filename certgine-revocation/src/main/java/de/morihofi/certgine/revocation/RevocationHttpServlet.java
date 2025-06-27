package de.morihofi.certgine.revocation;

import de.morihofi.certgine.revocation.endpoints.CRLEndpoint;
import de.morihofi.certgine.revocation.endpoints.OcspEndpointGet;
import de.morihofi.certgine.revocation.endpoints.OcspEndpointPost;
import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;

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
