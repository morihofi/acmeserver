/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.http;

import java.util.Locale;
import java.util.Optional;

/**
 * Supported proxy schemes.
 */
public enum ProxyScheme {
    /** HTTP proxy. */
    HTTP,
    /** SOCKS proxy. */
    SOCKS;

    /**
     * Resolves a {@link ProxyScheme} from a textual representation.
     *
     * @param value the scheme value, may be null
     * @return the matching {@link ProxyScheme}, or an empty {@link Optional} if not recognized
     */
    public static Optional<ProxyScheme> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "http" -> Optional.of(HTTP);
            case "socks", "socks4", "socks5" -> Optional.of(SOCKS);
            default -> Optional.empty();
        };
    }
}
