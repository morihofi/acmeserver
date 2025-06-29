/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired before processing an ACME API request. The event provides the
 * request path and HTTP method so listeners can perform logging or additional
 * security checks.
 */
@AllArgsConstructor
@Getter
public class BeforeAcmeApiRequestEvent extends AbstractEvent {
    private final String path;
    private final String method;
}
