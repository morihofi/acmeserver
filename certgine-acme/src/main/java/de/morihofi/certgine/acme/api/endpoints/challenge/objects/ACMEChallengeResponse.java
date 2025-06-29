/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.api.endpoints.challenge.objects;

import lombok.Data;

/**
 * Represents the response for an ACME challenge. This class encapsulates the details required
 * for the ACME challenge operations such as status, validation date, challenge URL, token, and type.
 */
@Data
public class ACMEChallengeResponse {

    /**
     * Status of the ACME challenge.
     */
    private String status;

    /**
     * Validated date, formatted as a string.
     */
    private String validated;

    /**
     * URL for challenge approval.
     */
    private String url;

    /**
     * Token to place.
     */
    private String token;

    /**
     * Type of the challenge.
     */
    private String type;
}
