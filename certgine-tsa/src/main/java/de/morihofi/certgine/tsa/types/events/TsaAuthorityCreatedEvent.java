/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.tsa.types.events;

import de.morihofi.certgine.tsa.types.entities.TsaAuthority;
import de.morihofi.certgine.types.events.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Event published when a new TSA authority is created. */
@AllArgsConstructor
@Getter
public class TsaAuthorityCreatedEvent extends AbstractEvent {
    private final TsaAuthority authority;
}
