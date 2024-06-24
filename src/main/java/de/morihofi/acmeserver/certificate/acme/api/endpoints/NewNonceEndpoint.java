/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import org.jetbrains.annotations.NotNull;

/**
 * A class representing a handler for handling new ACME nonces. This handler is responsible for responding to GET and HEAD requests by
 * providing appropriate HTTP statuses and headers, including a new "Replay-Nonce" header generated using the provided ACMEProvisioner.
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class NewNonceEndpoint implements Handler {
    /**
     * Instance for accessing the current provisioner
     */
    private final Provisioner provisioner;
    private final ServerInstance serverInstance;

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner.
     *
     * @param provisioner The ACME provisioner to use for generating nonces.
     */
    public NewNonceEndpoint(Provisioner provisioner,ServerInstance serverInstance) {
        this.provisioner = provisioner;
        this.serverInstance = serverInstance;
    }

    /**
     * Handles incoming HTTP requests and responds with appropriate statuses and headers.
     *
     * @param ctx The Context object representing the incoming HTTP request and response.
     */
    @Override
    public void handle(@NotNull Context ctx) {

        // Respond with a 204 No Content status for GET requests
        if (ctx.method() == HandlerType.GET) {
            ctx.status(204);
        }

        // Respond with a 200 OK status for HEAD requests
        if (ctx.method() == HandlerType.HEAD) {
            ctx.status(200);
        }

        // Set Cache-Control header to "no-store"
        ctx.header("Cache-Control", "no-store");

        // Set Link header to the ACME directory URL with "rel" attribute set to "index"
        ctx.header("Link", "<" + provisioner.getApiURL() + "/directory" + ">;rel=\"index\"");

        // Generate a new Replay-Nonce using the ACMEProvisioner and set it in the header
        ctx.header("Replay-Nonce", Crypto.createNonce(serverInstance));
    }
}
