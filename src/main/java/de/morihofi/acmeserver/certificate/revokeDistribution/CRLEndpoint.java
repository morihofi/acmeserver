package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class CRLEndpoint implements Handler {

    private final Provisioner provisioner;
    private final CRL crl;

    /**
     * Constructor for the CRLEndpoint class.
     * Initializes a new instance with a given Provisioner and CRL (Certificate Revocation List) object.
     *
     * @param provisioner the Provisioner instance to be associated with this endpoint
     * @param crl the CRL instance for handling certificate revocation related operations
     */
    public CRLEndpoint(Provisioner provisioner, CRL crl) {
        this.provisioner = provisioner;
        this.crl = crl;
    }


    /**
     * Handles an HTTP request by returning the current Certificate Revocation List (CRL) in the response.
     * Sets the HTTP status code to 200 (OK) and sets the appropriate headers for the CRL content type
     * and content length. The CRL data is written to the response's output stream.
     *
     * @param ctx The Context object representing the HTTP request and response.
     * @throws Exception if there is an issue with handling the HTTP request.
     */
    @Override
    public void handle(Context ctx) throws Exception {
        ctx.status(200);
        ByteBuffer buffer = ByteBuffer.wrap(crl.getCurrentCrlBytes());

        ctx.header("Content-Type", "application/pkix-crl");
        ctx.header("Content-Length", String.valueOf(buffer.capacity()));

        try (OutputStream out = ctx.res().getOutputStream()) {
            out.write(buffer.array());
        }

    }
}
