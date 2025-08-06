/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.order.objects;

import de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.util.List;

/**
 * Represents a response from the Certgine for an order request.
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
    private List<AcmeOrderIdentifier> identifiers;

    /**
     * The list of authorizations associated with the ACME order.
     */
    private List<String> authorizations;
}
