/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.endpoints;


import de.morihofi.certgine.server.common.intf.Handler;
import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.cryptography.ocsp.OcspProcessor;
import de.morihofi.certgine.types.intf.IServerInstance;
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
    final IServerInstance serverInstance;
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
        RevokedCertificate rc = AcmeOrder.getRevokedCertificate(serialNumber, provisioner.getName(), serverInstance);
        var crypto = serverInstance.getCryptoStoreManager();
        OCSPResp ocspResponse = OcspProcessor.processOCSPRequest(serialNumber, rc,
                crypto.getIntermediateCertificate(provisioner.getInternalUuid()),
                crypto.getIntermediateCertificateAuthorityKeyPair(provisioner.getInternalUuid()));

        // Sending the OCSP response
        context.contentType("application/ocsp-response");
        context.result(ocspResponse.getEncoded());
    }
}
