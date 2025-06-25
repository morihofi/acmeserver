/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.acme.api.endpoints.order;

import com.google.gson.Gson;
import de.morihofi.acmeserver.acme.api.abstractclass.AbstractAcmeEndpoint;
import de.morihofi.acmeserver.acme.api.objects.ACMERequestBody;

import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.acmeserver.types.database.entities.acme.HttpNonces;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoint for retrieving the certificate chain of an ACME order.
 * This class handles the request to fetch the PEM-encoded certificate chain for a given order ID.
 */
@Slf4j
public class OrderCertEndpoint extends AbstractAcmeEndpoint {

    /**
     * Constructs a new OrderCertEndpoint instance with the specified provisioner and server instance.
     *
     * @param serverInstance The server instance for managing server configurations and operations.
     */
    public OrderCertEndpoint(IServerInstance serverInstance) {
        super(serverInstance);
    }

    /**
     * Handles the request to retrieve the certificate chain for a specific order.
     * This method sets the appropriate headers and response body for the ACME certificate chain retrieval.
     *
     * @param ctx             The context of the HTTP request.
     * @param provisioner     The provisioner handling the ACME request.
     * @param gson            The Gson instance for JSON processing.
     * @param acmeRequestBody The body of the ACME request.
     * @throws Exception If an error occurs while processing the request.
     */
    @Override
    public void handleRequest(@NonNull HandlerContext ctx, @NonNull AcmeProvisioner provisioner, @NonNull Gson gson, @NonNull ACMERequestBody acmeRequestBody) throws Exception {
        String orderId = ctx.pathParam("orderId");

        ctx.header("Content-Type", "application/pem-certificate-chain");
        ctx.header("Replay-Nonce", HttpNonces.createNonce(getServerInstance()));
        // ctx.header("Link", "<" + provisioner.getAcmeApiURL() + "/directory" + ">;rel=\"index\"");

        AcmeOrder order = AcmeOrder.getAcmeOrder(orderId, getServerInstance());


        StringBuilder responseCertificateChainBuilder = new StringBuilder();

        List<X509Certificate> certChain = getCertificateChainOfACMEbyCertificateId(order,
                provisioner,
                getServerInstance());

        for (X509Certificate certificate : certChain) {
            responseCertificateChainBuilder.append(PemUtil.certificateToPEM(certificate.getEncoded()));
            responseCertificateChainBuilder.append("\n");
        }
        responseCertificateChainBuilder.append("\n");

        log.info("Returning certificate chain of intermediate and root-ca {}", certChain);

        responseCertificateChainBuilder.append("\n"); // Separator between certificates

        String responseCertificateChain = responseCertificateChainBuilder.toString();
        ctx.result(responseCertificateChain);
    }

    private List<X509Certificate> getCertificateChainOfACMEbyCertificateId(AcmeOrder order, AcmeProvisioner provisioner, IServerInstance serverInstance) throws KeyStoreException, CertificateException, IOException, UnrecoverableKeyException, NoSuchAlgorithmException {
        X509Certificate entityCertificate = PemUtil.parseCertificatePem(order.getCertificatePem());
        serverInstance.getCryptoStoreManager().getIntermediateCertificateAuthorityKeyPair(provisioner.getInternalUuid());
        X509Certificate[] chainFromStore = serverInstance.getCryptoStoreManager().getFullIntermediateCertificateChain(provisioner.getInternalUuid());

        List<X509Certificate> finalCertificateChain = new ArrayList<>();
        finalCertificateChain.add(entityCertificate);
        finalCertificateChain.addAll(List.of(chainFromStore));

        return finalCertificateChain;
    }
}
