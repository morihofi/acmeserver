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

package de.morihofi.acmeserver.acme.api.endpoints.order.objects;

import de.morihofi.acmeserver.types.api.acme.dns.Identifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.util.List;

/**
 * Represents a response from the ACME server for an order request.
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class AcmeOrderResponse {
    /**
     * The status of the ACME order.
     */
    private String status;

    /**
     * The expiration date of the ACME order.
     */
    private String expires;

    /**
     * The issuance date of the ACME order's certificate.
     */
    private String issued;

    /**
     * The finalize URL for the ACME order.
     */
    private String finalize;

    /**
     * The certificate URL for the ACME order.
     */
    private String certificate;

    /**
     * The list of identifiers associated with the ACME order.
     */
    private List<Identifier> identifiers;

    /**
     * The list of authorizations associated with the ACME order.
     */
    private List<String> authorizations;
}
