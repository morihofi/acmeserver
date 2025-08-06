/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets;

import de.morihofi.certgine.acme.servlets.handlerapi.AcmeBeforeHandler;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.*;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.account.AccountEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.account.NewAccountEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.FinalizeOrderEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.OrderCertEndpoint;
import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.OrderInfoEndpoint;
import de.morihofi.certgine.acme.types.entities.AcmeHttpNonce;
import de.morihofi.certgine.acme.types.events.AcmeExceptionEvent;
import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.server.common.intf.handler.AbstractExceptionHandler;
import de.morihofi.certgine.types.exception.ACMEException;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

/**
 * Servlet routing ACME requests to registered sub servlets.
 * This Servlet listens on path <code>/acme/*</code>
 */
@Slf4j
@ServletMount(servletMountPoint = "/acme/*", protect = true)
public class AcmeHttpServlet extends RoutableHttpServlet {

    private final CertgineModuleInstance moduleInstance;
    private final Clock clock;

    public AcmeHttpServlet(CertgineModuleInstance moduleInstance, Clock clock) {
        this.moduleInstance = moduleInstance;
        this.clock = clock;

        setExceptionHandler(new AbstractExceptionHandler() {
            @Override
            public void handle(Exception e, HandlerContext ctx) throws Exception {
                if (e instanceof ACMEException acmeException) {
                    ctx.header("Content-Type", "application/problem+json");
                    ctx.header("Replay-Nonce", AcmeHttpNonce.createNonce(moduleInstance.getModule().getServerInstance()));
                    ctx.status(acmeException.getHttpStatusCode());
                    ctx.json(acmeException.getErrorResponse());
                    log.error("ACME Exception thrown {} : {} ({})", acmeException.getClass().getSimpleName(), acmeException.getErrorResponse().getDetail(),
                            acmeException.getErrorResponse().getType());
                    moduleInstance.getModule().getServerInstance().getEventBus().publish(new AcmeExceptionEvent(acmeException));
                }
            }
        });

        // ACME Directory
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/acme/{provisioner}/directory", new DirectoryEndpoint(moduleInstance)));

        // New account
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/new-acct", new NewAccountEndpoint(moduleInstance)));

        // Key Change Endpoint (Account key rollover)
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/key-change", new KeyChangeEndpoint(moduleInstance)));

        // New Nonce
        getRouter().addHandler(new Endpoint(HandlerType.HEAD, "/acme/{provisioner}/acme/new-nonce", new NewNonceEndpoint(moduleInstance)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/acme/{provisioner}/acme/new-nonce", new NewNonceEndpoint(moduleInstance)));

        // Account Update
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/acct/{id}", new AccountEndpoint(moduleInstance)));

        // Create new Order
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/new-order", new NewOrderEndpoint(moduleInstance, clock)));

        // Challenge / Ownership verification
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/authz/{authorizationId}", new AuthzOwnershipEndpoint(moduleInstance, clock)));

        // Challenge Callback
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/chall/{challengeId}/{challengeType}", new ChallengeCallbackEndpoint(moduleInstance)));

        // Finalize endpoint
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/order/{orderId}/finalize", new FinalizeOrderEndpoint(moduleInstance)));

        // Order info Endpoint
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/order/{orderId}", new OrderInfoEndpoint(moduleInstance, clock)));

        // Get Order Certificate
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/order/{orderId}/cert", new OrderCertEndpoint(moduleInstance)));

        // Revoke certificate
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/acme/{provisioner}/acme/revoke-cert", new RevokeCertEndpoint(moduleInstance, clock)));

        getRouter().addBeforeHandler("/acme", new AcmeBeforeHandler());

    }

    public AcmeHttpServlet(CertgineModuleInstance moduleInstance) {
        this(moduleInstance, Clock.systemUTC());
    }
}
