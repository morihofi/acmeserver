/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints.objects;
import de.morihofi.certgine.acme.api.endpoints.NewOrderEndpoint;
import de.morihofi.certgine.types.api.acme.dns.Identifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/**
 * Request payload object for a new order, used in {@link NewOrderEndpoint}
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
@Getter
public class NewOrderRequestPayload {

    /**
     * List of identifiers, which the clients want to get certificates for
     */
    private List<Identifier> identifiers;

    /**
     * the desired notAfter property for the certificate
     */
    private Date notAfter;
}
