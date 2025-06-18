package de.morihofi.acmeserver.acme;

import de.morihofi.acmeserver.acme.api.endpoints.*;
import de.morihofi.acmeserver.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.acmeserver.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.acme.api.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.acmeserver.acme.api.endpoints.order.FinalizeOrderEndpoint;
import de.morihofi.acmeserver.acme.api.endpoints.order.OrderCertEndpoint;
import de.morihofi.acmeserver.acme.api.endpoints.order.OrderInfoEndpoint;
import de.morihofi.acmeserver.acme.revokeDistribution.CRLEndpoint;
import de.morihofi.acmeserver.acme.revokeDistribution.OcspEndpointGet;
import de.morihofi.acmeserver.acme.revokeDistribution.OcspEndpointPost;
import de.morihofi.acmeserver.server.common.intf.Endpoint;
import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.server.common.intf.RoutableHttpServlet;
import de.morihofi.acmeserver.server.common.intf.handler.AbstractExceptionHandler;
import de.morihofi.acmeserver.types.database.entities.HttpNonces;
import de.morihofi.acmeserver.types.events.AcmeExceptionEvent;
import de.morihofi.acmeserver.types.exception.ACMEException;
import de.morihofi.acmeserver.types.httpserver.HandlerType;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet routing ACME requests to registered sub servlets.
 * This Servlet listens on path <code>/acme/*</code>
 */
@Slf4j
public class AcmeHttpServlet extends RoutableHttpServlet {

    private final IServerInstance serverInstance;

    public AcmeHttpServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;

        setExceptionHandler(new AbstractExceptionHandler() {
            @Override
            public void handle(Exception e, HandlerContext ctx) throws Exception {
                if (e instanceof ACMEException acmeException) {
                    ctx.header("Content-Type", "application/problem+json");
                    ctx.header("Replay-Nonce", HttpNonces.createNonce(serverInstance));
                    ctx.json(acmeException.getErrorResponse());
                    log.error("ACME Exception thrown {} : {} ({})", acmeException.getClass().getSimpleName(), acmeException.getErrorResponse().getDetail(),
                            acmeException.getErrorResponse().getType());
                    serverInstance.getEventBus().publish(new AcmeExceptionEvent(acmeException));
                }
            }
        });


        // ACME Directory
        registerGetRoute("/acme/{provisioner}/directory", new DirectoryEndpoint(serverInstance));

        // CRL distribution
        registerGetRoute("/acme/crl/{provisioner}/certs-revoked.crl", new CRLEndpoint(serverInstance));

        // OCSP (Online Certificate Status Protocol) endpoints
        registerPostRoute("/acme/{provisioner}/ocsp", new OcspEndpointPost(serverInstance));
        registerGetRoute("/acme/{provisioner}/ocsp/{ocspRequest}", new OcspEndpointGet(serverInstance));

        // New account
        registerPostRoute("/acme/{provisioner}/acme/new-acct", new NewAccountEndpoint(serverInstance));

        // Key Change Endpoint (Account key rollover)
        registerPostRoute("/acme/{provisioner}/acme/key-change", new KeyChangeEndpoint(serverInstance));

        // New Nonce
        registerHeadRoute("/acme/{provisioner}/acme/new-nonce", new NewNonceEndpoint(serverInstance));
        registerGetRoute("/acme/{provisioner}/acme/new-nonce", new NewNonceEndpoint(serverInstance));

        // Account Update
        registerPostRoute("/acme/{provisioner}/acme/acct/{id}", new AccountEndpoint(serverInstance));

        // Create new Order
        registerPostRoute("/acme/{provisioner}/acme/new-order", new NewOrderEndpoint(serverInstance));

        // Challenge / Ownership verification
        registerPostRoute("/acme/{provisioner}/acme/authz/{authorizationId}", new AuthzOwnershipEndpoint(serverInstance));

        // Challenge Callback
        registerPostRoute("/acme/{provisioner}/acme/chall/{challengeId}/{challengeType}", new ChallengeCallbackEndpoint(serverInstance));

        // Finalize endpoint
        registerPostRoute("/acme/{provisioner}/acme/order/{orderId}/finalize", new FinalizeOrderEndpoint(serverInstance));

        // Order info Endpoint
        registerPostRoute("/acme/{provisioner}/acme/order/{orderId}", new OrderInfoEndpoint(serverInstance));

        // Get Order Certificate
        registerPostRoute("/acme/{provisioner}/acme/order/{orderId}/cert", new OrderCertEndpoint(serverInstance));

        // Revoke certificate
        registerPostRoute("/acme/{provisioner}/acme/revoke-cert", new RevokeCertEndpoint(serverInstance));


        getRouter().addBeforeHandler("/acme", new AcmeBeforeHandler());


    }

    private void registerHeadRoute(@NonNull String path, @NonNull Handler handler) {
        getRouter().addHandler(new Endpoint(HandlerType.HEAD, path, handler));
    }

    private void registerGetRoute(@NonNull String path, @NonNull Handler handler) {
        getRouter().addHandler(new Endpoint(HandlerType.GET, path, handler));
    }

    private void registerPostRoute(@NonNull String path, @NonNull Handler handler) {
        getRouter().addHandler(new Endpoint(HandlerType.POST, path, handler));
    }


}
