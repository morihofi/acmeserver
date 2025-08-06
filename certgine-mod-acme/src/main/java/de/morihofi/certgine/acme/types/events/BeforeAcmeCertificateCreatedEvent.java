/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.types.events.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Triggered right before the certificate for an ACME order is generated. This
 * allows subscribers to prepare any external systems that need to be aware of
 * upcoming certificate creation.
 */
@AllArgsConstructor
@Getter
public class BeforeAcmeCertificateCreatedEvent extends AbstractEvent {
    private final AcmeOrder order;
}
