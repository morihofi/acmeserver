/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.ssl.mozillasslconfig.response.version5dot1up;

import lombok.Getter;

/**
 * Represents the configurations for SSL settings including modern, old, and intermediate configurations.
 */
@Getter
public class Configurations {

    /**
     * The modern SSL configuration.
     */
    private Configuration modern;

    /**
     * The old SSL configuration.
     */
    private Configuration old;

    /**
     * The intermediate SSL configuration.
     */
    private Configuration intermediate;

}
