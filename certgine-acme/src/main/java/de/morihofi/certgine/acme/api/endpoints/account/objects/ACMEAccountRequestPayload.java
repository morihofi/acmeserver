/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
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
