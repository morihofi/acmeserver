/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation;

import de.morihofi.certgine.revocation.endpoints.CRLEndpoint;
import de.morihofi.certgine.revocation.endpoints.OcspEndpointGet;
import de.morihofi.certgine.revocation.endpoints.OcspEndpointPost;
import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet serving certificate revocation related endpoints like CRL and OCSP.
 */
@Slf4j
@ServletMount(servletMountPoint = "/revocation/*", protect = true)
public class RevocationHttpServlet extends RoutableHttpServlet {
    private final IServerInstance serverInstance;

    public RevocationHttpServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/revocation/crl/certs-revoked.crl", new CRLEndpoint(serverInstance)));
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/revocation/ocsp", new OcspEndpointPost(serverInstance)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/revocation/ocsp/{ocspRequest}", new OcspEndpointGet(serverInstance)));
    }
}
