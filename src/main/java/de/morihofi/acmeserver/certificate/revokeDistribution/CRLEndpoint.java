/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
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

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class CRLEndpoint implements Handler {

    private final Provisioner provisioner;

    /**
     * Constructor for the CRLEndpoint class. Initializes a new instance with a given Provisioner and CRL (Certificate Revocation List)
     * object.
     *
     * @param provisioner the Provisioner instance to be associated with this endpoint
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CRLEndpoint(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Handles an HTTP request by returning the current Certificate Revocation List (CRL) in the response. Sets the HTTP status code to 200
     * (OK) and sets the appropriate headers for the CRL content type and content length. The CRL data is written to the response's output
     * stream.
     *
     * @param ctx The Context object representing the HTTP request and response.
     * @throws Exception if there is an issue with handling the HTTP request.
     */
    @Override
    public void handle(Context ctx) throws Exception {
        ctx.status(200);
        ByteBuffer buffer =
                ByteBuffer.wrap(CRLScheduler.getCrlGeneratorForProvisioner(provisioner.getProvisionerName()).getCurrentCrlBytes());

        ctx.header("Content-Type", "application/pkix-crl");
        ctx.header("Content-Length", String.valueOf(buffer.capacity()));

        try (OutputStream out = ctx.res().getOutputStream()) {
            out.write(buffer.array());
        }
    }
}
