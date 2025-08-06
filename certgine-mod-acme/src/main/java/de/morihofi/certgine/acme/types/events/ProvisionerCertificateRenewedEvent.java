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
 * Dispatched after a provisioner's intermediate certificate has been renewed
 * and stored back in the KeyStore. Listeners may reload cached certificates
 * or inform administrators that new credentials are in place.
 */
@AllArgsConstructor
@Getter
public class ProvisionerCertificateRenewedEvent extends AbstractEvent {
    private final AcmeProvisioner provisioner;
}
