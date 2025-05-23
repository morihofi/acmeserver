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

package de.morihofi.acmeserver.certificate.acme.api.endpoints.order.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents a response from the ACME server for an order request.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEOrderResponse {
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

    /**
     * Get the status of the ACME order.
     *
     * @return The status string.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the status of the ACME order.
     *
     * @param status The status string to set.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Get the expiration date of the ACME order.
     *
     * @return The expiration date string.
     */
    public String getExpires() {
        return expires;
    }

    /**
     * Set the expiration date of the ACME order.
     *
     * @param expires The expiration date string to set.
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }

    /**
     * Get the finalize URL for the ACME order.
     *
     * @return The finalize URL string.
     */
    public String getFinalize() {
        return finalize;
    }

    /**
     * Set the finalize URL for the ACME order.
     *
     * @param finalize The finalize URL string to set.
     */
    public void setFinalize(String finalize) {
        this.finalize = finalize;
    }

    /**
     * Get the certificate URL for the ACME order.
     *
     * @return The certificate URL string.
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * Set the certificate URL for the ACME order.
     *
     * @param certificate The certificate URL string to set.
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * Get the list of identifiers associated with the ACME order.
     *
     * @return The list of Identifier objects.
     */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    /**
     * Set the list of identifiers associated with the ACME order.
     *
     * @param identifiers The list of Identifier objects to set.
     */
    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Get the list of authorizations associated with the ACME order.
     *
     * @return The list of authorization URL strings.
     */
    public List<String> getAuthorizations() {
        return authorizations;
    }

    /**
     * Set the list of authorizations associated with the ACME order.
     *
     * @param authorizations The list of authorization URL strings to set.
     */
    public void setAuthorizations(List<String> authorizations) {
        this.authorizations = authorizations;
    }

    /**
     * Get the issuance date of the ACME order's certificate.
     *
     * @return The issuance date string.
     */
    public String getIssued() {
        return issued;
    }

    /**
     * Set the issuance date of the ACME order's certificate.
     *
     * @param issued The issuance date string to set.
     */
    public void setIssued(String issued) {
        this.issued = issued;
    }
}
