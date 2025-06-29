/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Published whenever a new ACME provisioner is created during initialization or
 * via management interfaces. Useful for automation that must react to newly
 * available provisioners.
 */
@AllArgsConstructor
@Getter
public class ProvisionerCreatedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
