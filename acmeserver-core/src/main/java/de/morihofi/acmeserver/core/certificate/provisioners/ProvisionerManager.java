/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.certificate.provisioners;

import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.*;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.order.FinalizeOrderEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.order.OrderCertEndpoint;
import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.order.OrderInfoEndpoint;
import de.morihofi.acmeserver.core.certificate.revokeDistribution.CRLEndpoint;
import de.morihofi.acmeserver.core.certificate.revokeDistribution.CRLScheduler;
import de.morihofi.acmeserver.core.certificate.revokeDistribution.OcspEndpointGet;
import de.morihofi.acmeserver.core.certificate.revokeDistribution.OcspEndpointPost;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import de.morihofi.acmeserver.core.tools.http.HttpHeaderUtil;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;


import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Manages the registration and retrieval of ACME provisioners.
 *
 * <p>This class handles the registration of provisioners, setting up their respective endpoints for ACME operations, CRL distribution,
 * and OCSP handling. It also provides methods to retrieve registered provisioners by name.</p>
 */
@Slf4j
public class ProvisionerManager {

    /**
     * A set of registered provisioners.
     */
    private static final Set<Provisioner> provisioners = new HashSet<>();

    /**
     * Constructs the API prefix for a given provisioner name.
     *
     * @param provisionerName The name of the provisioner.
     * @return The API prefix for the provisioner.
     */
    private static String getProvisionerApiPrefix(String provisionerName) {
        return "/acme/" + provisionerName;
    }

    /**
     * Registers a provisioner with the specified Javalin application instance and server instance.
     *
     * <p>This method sets up the necessary endpoints for the provisioner and adds the provisioner to the CRL scheduler.</p>
     *
     * @param app            The Javalin application instance.
     * @param provisioner    The provisioner to register.
     * @param serverInstance The server instance.
     * @throws IllegalArgumentException If the provisioner is already registered.
     */
    public static void registerProvisioner(Javalin app, Provisioner provisioner, ServerInstance serverInstance) {
        if (provisioners.contains(provisioner)) {
            throw new IllegalArgumentException("Provisioner already registered");
        }

        // CRL generator
        CRLScheduler.addProvisionerToScheduler(provisioner, serverInstance);

        String prefix = getProvisionerApiPrefix(provisioner.getProvisionerName());

        // CRL distribution
        app.get(provisioner.getCrlPath(), new CRLEndpoint(provisioner));

        // OCSP (Online Certificate Status Protocol) endpoints
        app.post(provisioner.getOcspPath(), new OcspEndpointPost(provisioner));
        app.get(provisioner.getOcspPath() + "/{ocspRequest}", new OcspEndpointGet(provisioner));

        // Global ACME headers, inspired from Let's Encrypts Boulder
        app.before(prefix + "/*", context -> {
            // Disable caching for all ACME routes
            context.header("Cache-Control", "public, max-age=0, no-cache");

            if(!context.path().equals(prefix + "/directory")){
                context.header("Link", HttpHeaderUtil.buildLinkHeaderValue(provisioner.getAcmeApiURL() + "/directory", "index"));
            }

        });


        // ACME Directory
        app.get(prefix + "/directory", new DirectoryEndpoint(provisioner));

        // New account
        app.post(prefix + "/acme/new-acct", new NewAccountEndpoint(provisioner, serverInstance));

        // TODO: Key Change Endpoint (Account key rollover)
        app.post(prefix + "/acme/key-change", new NotImplementedEndpoint());
        app.get(prefix + "/acme/key-change", new NotImplementedEndpoint());

        // New Nonce
        app.head(prefix + "/acme/new-nonce", new NewNonceEndpoint(provisioner, serverInstance));
        app.get(prefix + "/acme/new-nonce", new NewNonceEndpoint(provisioner, serverInstance));

        // Account Update
        app.post(prefix + "/acme/acct/{id}", new AccountEndpoint(provisioner, serverInstance));

        // Create new Order
        app.post(prefix + "/acme/new-order", new NewOrderEndpoint(provisioner, serverInstance));

        // Challenge / Ownership verification
        app.post(prefix + "/acme/authz/{authorizationId}", new AuthzOwnershipEndpoint(provisioner, serverInstance));

        // Challenge Callback
        app.post(prefix + "/acme/chall/{challengeId}/{challengeType}", new ChallengeCallbackEndpoint(provisioner, serverInstance));

        // Finalize endpoint
        app.post(prefix + "/acme/order/{orderId}/finalize", new FinalizeOrderEndpoint(provisioner, serverInstance));

        // Order info Endpoint
        app.post(prefix + "/acme/order/{orderId}", new OrderInfoEndpoint(provisioner, serverInstance));

        // Get Order Certificate
        app.post(prefix + "/acme/order/{orderId}/cert", new OrderCertEndpoint(provisioner, serverInstance));

        // Revoke certificate
        app.post(prefix + "/acme/revoke-cert", new RevokeCertEndpoint(provisioner, serverInstance));

        log.info("Provisioner {} registered", provisioner.getProvisionerName());

        provisioners.add(provisioner);
    }

    /**
     * Retrieves a provisioner by name.
     *
     * @param provisionerName The name of the provisioner.
     * @return The provisioner with the specified name, or null if not found.
     */
    public static Provisioner getProvisionerForName(String provisionerName) {
        Optional<Provisioner> provisionerOptional = provisioners.stream()
                .filter(provisioner -> provisioner.getProvisionerName().equals(provisionerName))
                .findFirst();

        return provisionerOptional.orElse(null);
    }

    /**
     * Returns an unmodifiable set of registered provisioners.
     *
     * @return An unmodifiable set of provisioners.
     */
    public static Set<Provisioner> getProvisioners() {
        return Collections.unmodifiableSet(provisioners);
    }
}
