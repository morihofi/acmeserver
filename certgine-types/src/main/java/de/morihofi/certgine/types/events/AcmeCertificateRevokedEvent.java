/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.database.entities.acme.AcmeOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after a certificate has been revoked. Subscribers may refresh CRLs
 * or perform additional cleanup.
 */
@AllArgsConstructor
@Getter
public class AcmeCertificateRevokedEvent extends AbstractEvent {
    private final AcmeOrder order;
}
