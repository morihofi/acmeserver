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

package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.Req;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

/**
 * Handler for OCSP Requests using POST Method
 */
public class OcspEndpointPost implements Handler {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());
    /**
     * Instance for accessing the current provisioner
     */
    private final Provisioner provisioner;

    /**
     * Constructor for OcspEndpointPost class. Processes POST Requests. Initializes an instance with a specified Provisioner and CRL
     * generator.
     *
     * @param provisioner the Provisioner object to be used with this endpoint
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OcspEndpointPost(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Handles an HTTP request for OCSP (Online Certificate Status Protocol) by processing the provided OCSP request, checking the
     * revocation status for the specified certificate serial number, and sending the corresponding OCSP response.
     *
     * @param context The Context object representing the HTTP request and response.
     * @throws Exception if there is an issue with handling the HTTP request or processing the OCSP request.
     */
    @Override
    public void handle(@NotNull Context context) throws Exception {
        byte[] ocspRequestBytes = context.bodyAsBytes();
        OCSPReq ocspRequest = new OCSPReq(ocspRequestBytes);

        // Get serial number from request
        Req[] requestList = ocspRequest.getRequestList();
        if (requestList.length == 0) {
            throw new IllegalArgumentException("No request data in the OCSP request");
        }

        BigInteger serialNumber = requestList[0].getCertID().getSerialNumber();
        LOG.info("Checking revokation status for serial number {}", serialNumber);

        // Processing the request and creating the OCSP response
        OCSPResp ocspResponse =
                OcspHelper.processOCSPRequest(serialNumber, CRLScheduler.getCrlGeneratorForProvisioner(provisioner.getProvisionerName()),
                        provisioner);

        // Sending the OCSP response
        context.contentType("application/ocsp-response");
        context.result(ocspResponse.getEncoded());
    }
}
