/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.types.events.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event emitted after an account has been deactivated via the ACME API.
 * It contains the updated {@link AcmeAccount} so listeners can react to
 * the deactivation (e.g. revoke certificates or disable services).
 */
@AllArgsConstructor
@Getter
public class AcmeAccountDeactivatedEvent extends AbstractEvent {
    private final AcmeAccount account;
}
