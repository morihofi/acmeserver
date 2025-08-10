/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.endpoints;

import de.morihofi.certgine.cryptography.ocsp.OcspProcessor;
import de.morihofi.certgine.revocation.RevocationStore;
import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.types.intf.IServerInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.Req;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Handler for OCSP Requests using GET Method
 */
@Slf4j
public class OcspEndpointGet implements Handler {

    final IServerInstance serverInstance;

    /**
     * Creates a new OCSP GET endpoint.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OcspEndpointGet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles OCSP (Online Certificate Status Protocol) requests. This method decodes the OCSP request encoded in the URL path parameter,
     * extracts the certificate serial number, and generates an OCSP response. It then sends the OCSP response back to the client.
     *
     * @param ctx the Context object representing the HTTP request and response
     * @throws Exception if there's an error in processing the OCSP request or in generating the response. This includes cases like invalid
     *                   input, empty request, or issues with request parsing.
     */
    @Override
    public void handle(@NonNull HandlerContext ctx) throws Exception {

        String ocspRequestEncoded = ctx.pathParam("ocspRequest");
        if (ocspRequestEncoded.isEmpty()) {
            throw new IllegalArgumentException("No OCSP request provided");
        }
        String ocspRequestDecoded = URLDecoder.decode(ocspRequestEncoded, StandardCharsets.UTF_8);
        byte[] ocspRequestBytes;
        try {
            ocspRequestBytes = Base64.getDecoder().decode(ocspRequestDecoded);
        } catch (IllegalArgumentException ex) {
            ocspRequestBytes = Base64.getUrlDecoder().decode(ocspRequestDecoded);
        }

        OCSPReq ocspRequest = new OCSPReq(ocspRequestBytes);

        // Get serial number from request
        Req[] requestList = ocspRequest.getRequestList();
        if (requestList.length == 0) {
            throw new IllegalArgumentException("No request data in the OCSP request");
        }

        BigInteger serialNumber = requestList[0].getCertID().getSerialNumber();
        log.info("Checking revocation status for serial number {}", serialNumber);

        // Processing the request and creating the OCSP response
        RevokedCertificate rc = RevocationStore.getRevokedCertificate(serialNumber, serverInstance);
        OCSPResp ocspResponse = OcspProcessor.processOCSPRequest(serialNumber, rc,
                serverInstance.getCryptoStoreManager()
                        .getCertificateAuthorityX509Certificate(serverInstance.getRootCa()),
                serverInstance.getCryptoStoreManager()
                        .getCertificateAuthorityKeyPair(serverInstance.getRootCa()));

        // Sending the OCSP response
        ctx.contentType("application/ocsp-response");
        ctx.result(ocspResponse.getEncoded());
    }
}
