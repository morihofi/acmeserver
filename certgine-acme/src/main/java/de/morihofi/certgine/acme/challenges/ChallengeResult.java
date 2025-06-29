/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.challenges;

/**
 * Represents the result of a challenge verification.
 *
 * @param successful  Indicates whether the challenge was successful.
 * @param errorReason Reason for the error if the challenge was not successful.
 */
public record ChallengeResult(boolean successful, String errorReason) {
}
