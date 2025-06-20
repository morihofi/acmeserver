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

/**
 * Handler for OCSP Requests using POST Method
 */
@Slf4j
public class OcspEndpointPost implements Handler {

    /**
     * Instance for accessing the current provisioner
     */
    private final IServerInstance serverInstance;
    /**
     * Constructor for OcspEndpointPost class. Processes POST Requests. Initializes an instance with a specified Provisioner and CRL
     * generator.
     *
     * @param serverInstance the server instance object to be used with this endpoint
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OcspEndpointPost(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles an HTTP request for OCSP (Online Certificate Status Protocol) by processing the provided OCSP request, checking the
     * revocation status for the specified certificate serial number, and sending the corresponding OCSP response.
     *
     * @param context The Context object representing the HTTP request and response.
     * @throws Exception if there is an issue with handling the HTTP request or processing the OCSP request.
     */
    @Override
    public void handle(@NonNull HandlerContext context) throws Exception {
        String provisionerName = context.pathParam("provisioner");
        AcmeProvisioner provisioner = AcmeProvisioner.getForName(serverInstance, provisionerName);

        byte[] ocspRequestBytes = context.bodyAsBytes();
        OCSPReq ocspRequest = new OCSPReq(ocspRequestBytes);

        // Get serial number from request
        Req[] requestList = ocspRequest.getRequestList();
        if (requestList.length == 0) {
            throw new IllegalArgumentException("No request data in the OCSP request");
        }

        BigInteger serialNumber = requestList[0].getCertID().getSerialNumber();
        log.info("Checking revocation status for serial number {}", serialNumber);

        // Processing the request and creating the OCSP response
        OCSPResp ocspResponse = OcspProcessor.processOCSPRequest(serialNumber, provisioner, serverInstance);

        // Sending the OCSP response
        context.contentType("application/ocsp-response");
        context.result(ocspResponse.getEncoded());
    }
}
