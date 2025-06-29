/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.api.acme.challenge;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This enum holds ACME Challenge Types as specified in RFC 8555
 */
@AllArgsConstructor
@Getter
public enum AcmeChallengeType {
    /**
     * HTTP-01 Challenge
     */
    HTTP_01("http-01"),
    /**
     * DNS-01 Challenge
     */
    DNS_01("dns-01"),
    /**
     * TLS-ALPN-01 Challenge (unsupported, maybe implemented in future)
     */
    TLS_ALPN_01("tls-alpn-01");

    /**
     * ACME RFC 8555 friendly name of the challenge
     */
    private final String name;


}
