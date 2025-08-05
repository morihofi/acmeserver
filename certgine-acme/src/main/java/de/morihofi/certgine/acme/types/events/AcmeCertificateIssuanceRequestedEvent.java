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
 * Published when an ACME order requires certificate issuance. This event
 * allows asynchronous workers to pick up the order and generate the
 * certificate.
 */
@AllArgsConstructor
@Getter
public class AcmeCertificateIssuanceRequestedEvent extends AbstractEvent {
    private final AcmeOrder order;
}
