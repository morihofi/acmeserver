/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.exception.ACMEException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event published whenever an {@link de.morihofi.certgine.types.exception.ACMEException}
 * is thrown while processing a request. This allows listeners to react or log
 * exceptional conditions centrally.
 */
@AllArgsConstructor
@Getter
public class AcmeExceptionEvent extends AbstractEvent {
    private final ACMEException exception;
}
