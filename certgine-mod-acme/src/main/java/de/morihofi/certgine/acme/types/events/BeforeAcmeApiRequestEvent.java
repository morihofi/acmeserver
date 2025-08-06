/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.types.events.AbstractEvent;
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
