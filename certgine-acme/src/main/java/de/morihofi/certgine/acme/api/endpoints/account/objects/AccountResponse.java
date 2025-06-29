/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints.account.objects;

import de.morihofi.certgine.acme.api.endpoints.account.NewAccountEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.util.List;

/**
 * Represents the response structure for an ACME account, used primarily in the {@link NewAccountEndpoint}. This class encapsulates the
 * account details such as its status, contact information, and a link to the order endpoint.
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class AccountResponse {

    /**
     * The status of the ACME account. This status indicates the current state or condition of the account, such as 'active' or 'disabled'.
     */
    private String status;

    /**
     * The list of email contacts associated with the ACME account. Each contact is specified with a 'mailto:' prefix.
     */
    private List<String> contact;

    /**
     * The URL link to the Order Endpoint. This link is used to access the endpoint for order-related operations in the ACME protocol.
     */
    private String orders;
}
