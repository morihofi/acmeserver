/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.certgine.acme.api.endpoints.objects;

import de.morihofi.certgine.acme.api.endpoints.NewOrderEndpoint;
import de.morihofi.certgine.types.api.acme.dns.Identifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.util.List;

/**
 * Represents the response for a new certificate order request. This class encapsulates details of the response returned by the
 * {@link NewOrderEndpoint}. It includes information such as the order status, expiration, validity period, identifiers, authorization
 * details, and finalization URL.
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class NewOrderResponse {
    /**
     * The status of the order.
     */
    private String status;

    /**
     * The expiration date of the order.
     */
    private String expires;

    /**
     * The 'not before' date of the order.
     */
    private String notBefore;

    /**
     * The 'not after' date of the order.
     */
    private String notAfter;

    /**
     * The list of identifiers for the order.
     */
    private List<Identifier> identifiers;

    /**
     * The list of authorization URLs for the order.
     */
    private List<String> authorizations;

    /**
     * The URL used to finalize the order.
     */
    private String finalize;
}
