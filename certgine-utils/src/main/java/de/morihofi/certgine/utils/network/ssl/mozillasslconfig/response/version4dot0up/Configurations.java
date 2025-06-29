/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.ssl.mozillasslconfig.response.version4dot0up;

import lombok.Getter;

/**
 * Represents the SSL configurations for different levels: modern, old, and intermediate.
 * This class contains configuration details for each level.
 */
@Getter
public class Configurations {

    /**
     * Configuration object for modern SSL configurations.
     */
    private Configuration modern;

    /**
     * Configuration object for old SSL configurations.
     */
    private Configuration old;

    /**
     * Configuration object for intermediate SSL configurations.
     */
    private Configuration intermediate;

}
