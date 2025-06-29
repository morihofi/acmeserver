/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme;

import de.morihofi.certgine.acme.api.AcmeBeforeHandler;
import de.morihofi.certgine.acme.api.endpoints.*;
import de.morihofi.certgine.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.certgine.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.certgine.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.certgine.acme.api.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.certgine.acme.api.endpoints.order.FinalizeOrderEndpoint;
import de.morihofi.certgine.acme.api.endpoints.order.OrderCertEndpoint;
import de.morihofi.certgine.acme.api.endpoints.order.OrderInfoEndpoint;
import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.server.common.intf.handler.AbstractExceptionHandler;
import de.morihofi.certgine.types.database.entities.acme.HttpNonces;
import de.morihofi.certgine.types.events.AcmeExceptionEvent;
import de.morihofi.certgine.types.exception.ACMEException;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet routing ACME requests to registered sub servlets.
 * This Servlet listens on path <code>/acme/*</code>
 */
@Slf4j
@ServletMount(servletMountPoint = "/acme/*", protect = true)
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
                    ctx.status(acmeException.getHttpStatusCode());
                    ctx.json(acmeException.getErrorResponse());
                    log.error("ACME Exception thrown {} : {} ({})", acmeException.getClass().getSimpleName(), acmeException.getErrorResponse().getDetail(),
                            acmeException.getErrorResponse().getType());
                    serverInstance.getEventBus().publish(new AcmeExceptionEvent(acmeException));
                }
            }
        });

        // ACME Directory
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/acme/{provisioner}/directory", new DirectoryEndpoint(serverInstance)));

        // New account
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/new-acct", new NewAccountEndpoint(serverInstance)));

        // Key Change Endpoint (Account key rollover)
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/key-change", new KeyChangeEndpoint(serverInstance)));

        // New Nonce
        getRouter().addHandler(new Endpoint(HandlerType.HEAD, "/acme/{provisioner}/acme/new-nonce", new NewNonceEndpoint(serverInstance)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/acme/{provisioner}/acme/new-nonce", new NewNonceEndpoint(serverInstance)));

        // Account Update
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/acct/{id}", new AccountEndpoint(serverInstance)));

        // Create new Order
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/new-order", new NewOrderEndpoint(serverInstance)));

        // Challenge / Ownership verification
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/authz/{authorizationId}", new AuthzOwnershipEndpoint(serverInstance)));

        // Challenge Callback
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/chall/{challengeId}/{challengeType}", new ChallengeCallbackEndpoint(serverInstance)));

        // Finalize endpoint
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/order/{orderId}/finalize", new FinalizeOrderEndpoint(serverInstance)));

        // Order info Endpoint
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/order/{orderId}", new OrderInfoEndpoint(serverInstance)));

        // Get Order Certificate
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/order/{orderId}/cert", new OrderCertEndpoint(serverInstance)));

        // Revoke certificate
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/revoke-cert", new RevokeCertEndpoint(serverInstance)));

        getRouter().addBeforeHandler("/acme", new AcmeBeforeHandler());

    }
}
