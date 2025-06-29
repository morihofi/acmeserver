/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config;


import lombok.Data;

import java.io.Serializable;

/**
 * Represents configuration for HTTP and HTTPS ports.
 */
@Data
public class Ports implements Serializable {
    /**
     * HTTP Port
     */
    private int http = 80;
    /**
     * HTTPS Port
     */
    private int https = 443;
}
