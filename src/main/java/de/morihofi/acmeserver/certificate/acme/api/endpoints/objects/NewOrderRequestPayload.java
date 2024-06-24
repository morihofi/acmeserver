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

package de.morihofi.acmeserver.certificate.acme.api.endpoints.objects;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.NewOrderEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Date;
import java.util.List;

/**
 * Request payload object for a new order, used in {@link NewOrderEndpoint}
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class NewOrderRequestPayload {

    /**
     * List of identifiers, which the clients want to get certificates for
     */
    private List<Identifier> identifiers;

    /**
     * the desired notAfter property for the certificate
     */
    private Date notAfter;

    /**
     * Retrieves the list of identifiers associated with this instance.
     * Identifiers typically represent entities such as domain names or email addresses,
     * which are relevant to the context of this object.
     *
     * @return A list of {@link Identifier} objects representing the identifiers.
     */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    /**
     * Sets the list of identifiers for this instance.
     * This method allows updating the identifiers, which could be domain names,
     * email addresses, or other relevant entities.
     *
     * @param value A list of {@link Identifier} objects to set as the new identifiers.
     */

    public void setIdentifiers(List<Identifier> value) {
        this.identifiers = value;
    }

    /**
     * Retrieves the notAfter field associated with this instance.
     *
     * @return The notAfter field as {@link Date}.
     */
    public Date getNotAfter() {
        return notAfter;
    }

    /**
     * Sets the notAfter property for this instance.
     * This method allows updating the notAfter.
     *
     * @param value The new notAfter {@link Date} object.
     */
    public void setNotAfter(Date value) {
        this.notAfter = value;
    }

}
