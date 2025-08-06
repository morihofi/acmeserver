/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.types.events.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired when an existing ACME provisioner is removed from the system. This
 * allows cleanup of resources that were tied to that provisioner.
 */
@AllArgsConstructor
@Getter
public class ProvisionerDeletedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
