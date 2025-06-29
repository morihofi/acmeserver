/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.server;

/**
 * Custom flag enum that change behaviour of the server
 */
public enum StartupFlag {
    /**
     * Enables the async certificate issuing, that is currently a buggy in certbot.
     */
    USE_ASYNC_CERTIFICATE_ISSUING
}
