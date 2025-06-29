/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents the configuration for Mozilla SSL settings.
 * This class holds the settings for enabling Mozilla SSL configuration,
 * specifying the version of the SSL configuration guidelines, and the configuration name.
 */
@Data
public class MozillaSslConfig implements Serializable {

    /**
     * Indicates whether Mozilla SSL-Config settings are enabled.
     */
    private boolean enabled = false;

    /**
     * Specifies the version of the Mozilla SSL-Config guidelines to use.
     */
    private String version = "5.7";

    /**
     * Specifies the name of the Mozilla SSL-Config configuration to use.
     */
    private String configuration = "intermediate";

}
