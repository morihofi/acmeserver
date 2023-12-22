package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.Req;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Base64;

public class OcspEndpointGet implements Handler {
    /**
     * Instance for accessing the current provisioner
     */
    private final Provisioner provisioner;

    /**
     * Instance of the CRL Generator
     */
    private final CRL crlGenerator;

    /**
     * Logger
     */
    public final Logger log = LogManager.getLogger(getClass());

    /**
     * Constructor for OcspEndpointGet class. Processes GET Requests
     * Creates an instance with specified Provisioner and CRL generator.
     *
     * @param provisioner  the Provisioner instance for OCSP handling
     * @param crlGenerator the CRL (Certificate Revocation List) generator for managing revoked certificates
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OcspEndpointGet(Provisioner provisioner, CRL crlGenerator) {
        this.provisioner = provisioner;
        this.crlGenerator = crlGenerator;
    }

    /**
     * Handles OCSP (Online Certificate Status Protocol) requests.
     * This method decodes the OCSP request encoded in the URL path parameter,
     * extracts the certificate serial number, and generates an OCSP response.
     * It then sends the OCSP response back to the client.
     *
     * @param ctx the Context object representing the HTTP request and response
     * @throws Exception if there's an error in processing the OCSP request or in generating the response.
     *                   This includes cases like invalid input, empty request, or issues with request parsing.
     */
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String ocspRequestEncoded = ctx.pathParam("ocspRequest");
        if (ocspRequestEncoded.isEmpty()) {
            throw new IllegalArgumentException("No OCSP request provided");
        }
        byte[] ocspRequestBytes = Base64.getDecoder().decode(ocspRequestEncoded);

        OCSPReq ocspRequest = new OCSPReq(ocspRequestBytes);

        // Get serial number from request
        Req[] requestList = ocspRequest.getRequestList();
        if (requestList.length == 0) {
            throw new IllegalArgumentException("No request data in the OCSP request");
        }

        BigInteger serialNumber = requestList[0].getCertID().getSerialNumber();
        log.info("Checking revokation status for serial number {}", serialNumber);

        // Processing the request and creating the OCSP response
        OCSPResp ocspResponse = OcspHelper.processOCSPRequest(serialNumber, crlGenerator, provisioner);

        // Sending the OCSP response
        ctx.contentType("application/ocsp-response");
        ctx.result(ocspResponse.getEncoded());
    }
}
