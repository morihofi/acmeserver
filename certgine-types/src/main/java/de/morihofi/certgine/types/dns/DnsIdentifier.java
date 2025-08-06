/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.dns;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * DNS Identifier used for Certificate Generation and other DNS related stuff
 */
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class DnsIdentifier {

    /**
     * Type of the DNS identifier, mostly <code>dns</code>. Can also be <code>ip</code>
     */
    private final IDENTIFIER_TYPE type;

    /**
     * Value of the identifier, so it is the DNS Name
     */
    private final String value;


    public enum IDENTIFIER_TYPE {
        DNS, IP;
    }
}
