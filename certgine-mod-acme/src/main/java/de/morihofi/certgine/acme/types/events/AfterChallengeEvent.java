/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.acme.challenges.ChallengeResult;
import de.morihofi.certgine.acme.types.api.AcmeChallengeType;
import de.morihofi.certgine.types.events.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired after the ownership challenge has been processed. Contains the
 * {@link ChallengeResult} indicating whether validation succeeded or failed
 * and the optional error reason.
 */
@AllArgsConstructor
@Getter
public class AfterChallengeEvent extends AbstractEvent {
    private final AcmeChallengeType method;
    private final String challengeId;
    private final ChallengeResult result;
}
