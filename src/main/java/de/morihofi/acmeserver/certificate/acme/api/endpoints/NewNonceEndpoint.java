package de.morihofi.acmeserver.certificate.acme.api.endpoints;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.tools.crypto.Crypto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import org.jetbrains.annotations.NotNull;

/**
 * A class representing a handler for handling new ACME nonces.
 * This handler is responsible for responding to GET and HEAD requests
 * by providing appropriate HTTP statuses and headers, including a new
 * "Replay-Nonce" header generated using the provided ACMEProvisioner.
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class NewNonceEndpoint implements Handler {
    /**
     * Instance for accessing the current provisioner
     */
    private final Provisioner provisioner;

    /**
     * Constructs a NewNonce handler with the specified ACME provisioner.
     *
     * @param provisioner The ACME provisioner to use for generating nonces.
     */
    public NewNonceEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Handles incoming HTTP requests and responds with appropriate statuses and headers.
     *
     * @param ctx The Context object representing the incoming HTTP request and response.
     * @throws Exception If an error occurs while handling the request.
     */
    @Override
    public void handle(@NotNull Context ctx) throws Exception {

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
        ctx.header("Replay-Nonce", Crypto.createNonce());

    }
}

