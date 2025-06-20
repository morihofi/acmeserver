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

package de.morihofi.acmeserver.revocation.endpoints;


import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.cryptography.ocsp.OcspProcessor;
import de.morihofi.acmeserver.types.intf.IServerInstance;
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

    private final IServerInstance serverInstance;

    /**
     * Constructor for OcspEndpointGet class. Processes GET Requests Creates an instance with specified Provisioner and CRL generator.
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
        String provisionerName = ctx.pathParam("provisioner");


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
        OCSPResp ocspResponse = OcspProcessor.processOCSPRequest(serialNumber, AcmeProvisioner.getForName(serverInstance, provisionerName), serverInstance);

        // Sending the OCSP response
        ctx.contentType("application/ocsp-response");
        ctx.result(ocspResponse.getEncoded());
    }
}
