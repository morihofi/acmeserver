package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
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
    private final Provisioner provisioner;
    private final CRL crlGenerator;
    public final Logger log = LogManager.getLogger(getClass());


    public OcspEndpointGet(Provisioner provisioner, CRL crlGenerator) {
        this.provisioner = provisioner;
        this.crlGenerator = crlGenerator;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String ocspRequestEncoded = ctx.pathParam("ocspRequest");
        if (ocspRequestEncoded == null || ocspRequestEncoded.isEmpty()) {
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
        log.info("Checking revokation status for serial number " + serialNumber);

        // Processing the request and creating the OCSP response
        OCSPResp ocspResponse = OcspHelper.processOCSPRequest(serialNumber, crlGenerator, provisioner);

        // Sending the OCSP response
        ctx.contentType("application/ocsp-response");
        ctx.result(ocspResponse.getEncoded());
    }
}
