/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints;


import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.database.entities.acme.HttpNonces;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.NonNull;

import java.net.HttpURLConnection;


/**
 * A class representing a handler for handling new ACME nonces. This handler is responsible for responding to GET and HEAD requests by
 * providing appropriate HTTP statuses and headers, including a new "Replay-Nonce" header generated using the provided ACMEProvisioner.
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class NewNonceEndpoint implements Handler {

    /**
     * Instance for accessing the server instance.
     */
    private final IServerInstance serverInstance;

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner.
     *
     * @param serverInstance The server instance to use for generating nonces.
     */
    public NewNonceEndpoint(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles incoming HTTP requests and responds with appropriate statuses and headers.
     *
     * @param ctx The Context object representing the incoming HTTP request and response.
     */
    @Override
    public void handle(@NonNull HandlerContext ctx) {

        // Respond with a 204 No Content status for GET requests
        if (ctx.method() == HandlerType.GET) {
            ctx.status(HttpURLConnection.HTTP_NO_CONTENT);
        }

        // Respond with a 200 OK status for HEAD requests
        if (ctx.method() == HandlerType.HEAD) {
            ctx.status(HttpURLConnection.HTTP_OK);
        }

        // Set Cache-Control header to "no-store"
        ctx.header("Cache-Control", "no-store");

        // Generate a new Replay-Nonce using the ACMEProvisioner and set it in the header
        ctx.header("Replay-Nonce", HttpNonces.createNonce(serverInstance));
    }
}
