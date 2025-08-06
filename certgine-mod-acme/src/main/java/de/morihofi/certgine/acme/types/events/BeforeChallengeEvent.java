/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types.events;

import de.morihofi.certgine.acme.types.api.AcmeChallengeType;
import de.morihofi.certgine.types.events.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Emitted directly before a domain ownership challenge is validated. It
 * includes the challenge method and identifier so subscribers can prepare
 * resources or log the attempt.
 */
@AllArgsConstructor
@Getter
public class BeforeChallengeEvent extends AbstractEvent {
    private final AcmeChallengeType method;
    private final String challengeId;
}
