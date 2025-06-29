/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
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
