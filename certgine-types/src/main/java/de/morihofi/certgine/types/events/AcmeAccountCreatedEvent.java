/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.database.entities.acme.AcmeAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired after a new ACME account was created and stored in the database.
 * Subscribers can inspect the {@link AcmeAccount} instance to perform
 * additional tasks such as audit logging.
 */
@AllArgsConstructor
@Getter
public class AcmeAccountCreatedEvent extends AbstractEvent {
    private final AcmeAccount account;
}
