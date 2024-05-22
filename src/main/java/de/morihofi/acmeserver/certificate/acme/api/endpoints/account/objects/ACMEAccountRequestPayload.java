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

package de.morihofi.acmeserver.certificate.acme.api.endpoints.account.objects;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.account.NewAccountEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents the HTTP request body for creating or managing an account in the {@link AccountEndpoint} and {@link NewAccountEndpoint}. This
 * class encapsulates the details required for ACME account operations, such as contact information and agreement to terms of service.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ACMEAccountRequestPayload {

    /**
     * List of E-Mail contacts in the ACME account. Each contact is specified with a 'mailto:' prefix.
     */
    private List<String> contact;

    /**
     * Indicates whether the user has agreed to the Terms of Service of the ACME Provisioner.
     */
    private boolean termsOfServiceAgreed;

    private String status;

    /**
     * Retrieves the list of email contacts associated with the ACME account. Each email in the list is prefixed with 'mailto:'.
     *
     * @return A list of {@code String} representing the email contacts.
     */
    public List<String> getContact() {
        return contact;
    }

    /**
     * Sets the list of email contacts for the ACME account. Each email should be prefixed with 'mailto:'.
     *
     * @param contact A list of {@code String} to set as the email contacts.
     */
    public void setContact(List<String> contact) {
        this.contact = contact;
    }

    /**
     * Checks if the user has agreed to the Terms of Service. This boolean flag indicates the user's consent to the terms.
     *
     * @return {@code true} if the Terms of Service are agreed, otherwise {@code false}.
     */
    public boolean getTermsOfServiceAgreed() {
        return termsOfServiceAgreed;
    }

    public boolean isTermsOfServiceAgreed() {
        return termsOfServiceAgreed;
    }

    /**
     * Sets the user's agreement status to the Terms of Service. This method updates the user's consent to the terms.
     *
     * @param termsOfServiceAgreed The agreement status to set, {@code true} if agreed, otherwise {@code false}.
     */
    public void setTermsOfServiceAgreed(boolean termsOfServiceAgreed) {
        this.termsOfServiceAgreed = termsOfServiceAgreed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
