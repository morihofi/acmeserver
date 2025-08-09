/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.acme.types.entities.AcmeAccount;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.types.events.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event published when a new ACME order is pending and a notification should be sent.
 */
@AllArgsConstructor
@Getter
public class AcmePendingOrderEvent extends AbstractEvent {
    private final AcmeAccount account;
    private final AcmeOrder order;
}
