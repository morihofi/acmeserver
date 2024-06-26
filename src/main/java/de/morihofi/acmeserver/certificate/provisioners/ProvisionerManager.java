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

package de.morihofi.acmeserver.certificate.provisioners;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.DirectoryEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.NewNonceEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.NewOrderEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.NotImplementedEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.RevokeCertEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.authz.AuthzOwnershipEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.challenge.ChallengeCallbackEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.FinalizeOrderEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.OrderCertEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.order.OrderInfoEndpoint;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRLEndpoint;
import de.morihofi.acmeserver.certificate.revokeDistribution.CRLScheduler;
import de.morihofi.acmeserver.certificate.revokeDistribution.OcspEndpointGet;
import de.morihofi.acmeserver.certificate.revokeDistribution.OcspEndpointPost;
import io.javalin.Javalin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ProvisionerManager {

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().getClass());

    private final static Set<Provisioner> provisioners = new HashSet<>();

    private static String getProvisionerApiPrefix(String provisionerName) {
        return "/acme/" + provisionerName;
    }

    public static void registerProvisioner(Javalin app, Provisioner provisioner) {
        if (provisioners.contains(provisioner)) {
            throw new IllegalArgumentException("Provisioner already registered");
        }

        // CRL generator
        CRLScheduler.addProvisionerToScheduler(provisioner);

        String prefix = getProvisionerApiPrefix(provisioner.getProvisionerName());

        // CRL distribution
        app.get(provisioner.getCrlPath(), new CRLEndpoint(provisioner));

        // OCSP (Online Certificate Status Protocol) endpoints
        app.post(provisioner.getOcspPath(), new OcspEndpointPost(provisioner));
        app.get(provisioner.getOcspPath() + "/{ocspRequest}", new OcspEndpointGet(provisioner));

        // ACME Directory
        app.get(prefix + "/directory", new DirectoryEndpoint(provisioner));

        // New account
        app.post(prefix + "/acme/new-acct", new NewAccountEndpoint(provisioner));

        // TODO: Key Change Endpoint (Account key rollover)
        app.post(prefix + "/acme/key-change", new NotImplementedEndpoint());
        app.get(prefix + "/acme/key-change", new NotImplementedEndpoint());

        // New Nonce
        app.head(prefix + "/acme/new-nonce", new NewNonceEndpoint(provisioner));
        app.get(prefix + "/acme/new-nonce", new NewNonceEndpoint(provisioner));

        // Account Update
        app.post(prefix + "/acme/acct/{id}", new AccountEndpoint(provisioner));

        // Create new Order
        app.post(prefix + "/acme/new-order", new NewOrderEndpoint(provisioner));

        // Challenge / Ownership verification
        app.post(prefix + "/acme/authz/{authorizationId}", new AuthzOwnershipEndpoint(provisioner));

        // Challenge Callback
        app.post(prefix + "/acme/chall/{challengeId}/{challengeType}", new ChallengeCallbackEndpoint(provisioner));

        // Finalize endpoint
        app.post(prefix + "/acme/order/{orderId}/finalize", new FinalizeOrderEndpoint(provisioner));

        // Order info Endpoint
        app.post(prefix + "/acme/order/{orderId}", new OrderInfoEndpoint(provisioner));

        // Get Order Certificate
        app.post(prefix + "/acme/order/{orderId}/cert", new OrderCertEndpoint(provisioner));

        // Revoke certificate
        app.post(prefix + "/acme/revoke-cert", new RevokeCertEndpoint(provisioner));

        log.info("Provisioner {} registered", provisioner.getProvisionerName());

        provisioners.add(provisioner);
    }

    /**
     * Get a Provisioner object by name, otherwise null
     * @param provisionerName Name of the Provisioner
     * @return Provisioner object
     */
    public static Provisioner getProvisionerForName(String provisionerName) {
        Optional<Provisioner> provisionerOptional = provisioners.stream()
                .filter(provisioner -> provisioner.getProvisionerName().equals(provisionerName))
                .findFirst();

        return provisionerOptional.orElse(null);
    }

    public static Set<Provisioner> getProvisioners() {
        return Collections.unmodifiableSet(provisioners);
    }
}
