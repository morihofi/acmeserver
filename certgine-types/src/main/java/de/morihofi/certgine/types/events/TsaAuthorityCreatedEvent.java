/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Event published when a new TSA authority is created. */
@AllArgsConstructor
@Getter
public class TsaAuthorityCreatedEvent extends AbstractEvent {
    private final TsaAuthority authority;
}
