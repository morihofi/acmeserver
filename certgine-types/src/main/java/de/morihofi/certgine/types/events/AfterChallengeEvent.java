/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import de.morihofi.certgine.types.api.acme.challenge.AcmeChallengeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after the ownership challenge has been processed. Contains the result
 * indicating whether validation succeeded.
 */
@AllArgsConstructor
@Getter
public class AfterChallengeEvent extends AbstractEvent {
    private final AcmeChallengeType method;
    private final String challengeId;
    private final boolean successful;
}
