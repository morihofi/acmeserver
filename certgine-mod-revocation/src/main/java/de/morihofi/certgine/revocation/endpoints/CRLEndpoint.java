/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.endpoints;

import de.morihofi.certgine.revocation.crl.CrlStore;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class CRLEndpoint implements Handler {

    private final IServerInstance serverInstance;

    /**
     * Constructor for the CRLEndpoint class. Initializes a new instance with a given Provisioner and CRL (Certificate Revocation List)
     * object.
     *
     * @param serverInstance the instance to be associated with this endpoint
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CRLEndpoint(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles an HTTP request by returning the current Certificate Revocation List (CRL) in the response. Sets the HTTP status code to 200
     * (OK) and sets the appropriate headers for the CRL content type and content length. The CRL data is written to the response's output
     * stream.
     *
     * @param ctx The Context object representing the HTTP request and response.
     * @throws Exception if there is an issue with handling the HTTP request.
     */
    @Override
    public void handle(@NonNull HandlerContext ctx) throws Exception {

        String provisionerName = ctx.pathParam("provisioner");

        ctx.status(200);
        ByteBuffer buffer = ByteBuffer.wrap(CrlStore.getCrlForProvisioner(provisionerName).getCrlAsBytes());

        ctx.header("Content-Type", "application/pkix-crl");
        ctx.header("Content-Length", String.valueOf(buffer.capacity()));

        try (OutputStream out = ctx.response().getOutputStream()) {
            out.write(buffer.array());
        }
    }
}
