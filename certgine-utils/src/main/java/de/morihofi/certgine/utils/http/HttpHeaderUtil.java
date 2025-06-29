/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.http;

import lombok.NonNull;

/**
 * Utility class for HTTP header operations.
 */
public class HttpHeaderUtil {

    /**
     * Builds a properly formatted link header value string.
     * <p>
     * This method formats a URL and a relation type into a link header
     * value following the format specified in RFC 8288.
     *
     * @param url      the URL to be used in the link header
     * @param relation the relation type (e.g., "prev", "next", "index", "up", ...)
     * @return the formatted link header value
     */
    @NonNull
    public static String buildLinkHeaderValue(@NonNull String url, @NonNull String relation) {
        return String.format("<%s>; rel=\"%s\"", url, relation);
    }
}
