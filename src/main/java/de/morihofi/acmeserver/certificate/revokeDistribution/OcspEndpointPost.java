package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.ocsp.*;
import org.jetbrains.annotations.NotNull;
import java.math.BigInteger;

public class OcspEndpointPost implements Handler {

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
     * Constructor for OcspEndpointPost class. Processes POST Requests.
     * Initializes an instance with a specified Provisioner and CRL generator.
     *
     * @param provisioner  the Provisioner object to be used with this endpoint
     * @param crlGenerator the CRL (Certificate Revocation List) generator for managing revoked certificates
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OcspEndpointPost(Provisioner provisioner, CRL crlGenerator) {
        this.provisioner = provisioner;
        this.crlGenerator = crlGenerator;
    }


    /**
     * Handles an HTTP request for OCSP (Online Certificate Status Protocol) by processing the provided OCSP request,
     * checking the revocation status for the specified certificate serial number, and sending the corresponding OCSP response.
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
        log.info("Checking revokation status for serial number {}", serialNumber);

        // Processing the request and creating the OCSP response
        OCSPResp ocspResponse = OcspHelper.processOCSPRequest(serialNumber, crlGenerator, provisioner);

        // Sending the OCSP response
        context.contentType("application/ocsp-response");
        context.result(ocspResponse.getEncoded());
    }


}
