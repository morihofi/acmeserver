/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.servlets.handlerapi.endpoints.objects;

import de.morihofi.certgine.acme.servlets.handlerapi.endpoints.NewOrderEndpoint;
import de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier;
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
    private List<AcmeOrderIdentifier> identifiers;

    /**
     * The list of authorization URLs for the order.
     */
    private List<String> authorizations;

    /**
     * The URL used to finalize the order.
     */
    private String finalize;
}
