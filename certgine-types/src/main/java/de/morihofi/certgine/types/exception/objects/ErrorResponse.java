/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.exception.objects;

import lombok.Builder;
import lombok.Getter;

/**
 * A class representing an error response in an API or web service. It typically contains information about the error type and additional
 * details.
 */
@Getter
@Builder
public class ErrorResponse {
    /**
     * RFC compliant error code
     */
    private String type;
    /**
     * Get additional details about the error.
     */
    private String detail;
}
