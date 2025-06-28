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

package de.morihofi.certgine.acme.api.endpoints.account.objects;

import de.morihofi.certgine.acme.api.endpoints.account.AccountEndpoint;
import de.morihofi.certgine.acme.api.endpoints.account.NewAccountEndpoint;
import de.morihofi.certgine.acme.api.endpoints.account.objects.ExternalAccountBinding;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.util.List;

/**
 * Represents the HTTP request body for creating or managing an account in the {@link AccountEndpoint} and {@link NewAccountEndpoint}. This
 * class encapsulates the details required for ACME account operations, such as contact information and agreement to terms of service.
 */
@Data
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

    /**
     * External account binding information if provided.
     */
    private ExternalAccountBinding externalAccountBinding;

    /**
     * The status of the ACME account, e.g., active, deactivated.
     */
    private String status;
}
