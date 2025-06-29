/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.ssl.mozillasslconfig.response.version5dot1up;

/**
 * Represents the response for Mozilla SSL configuration version 5.1 and up.
 */
public class MozillaSslConfiguration5dot1upResponse {

    /**
     * The configurations for the SSL settings.
     */
    private Configurations configurations;

    /**
     * The URL reference for more information about the SSL configurations.
     */
    private String href;

    /**
     * The version of the Mozilla SSL configuration.
     */
    private double version;

    /**
     * Gets the SSL configurations.
     *
     * @return The configurations object containing SSL settings.
     */
    public Configurations getConfigurations() {
        return configurations;
    }

    /**
     * Gets the URL reference for more information about the SSL configurations.
     *
     * @return The URL as a string.
     */
    public String getHref() {
        return href;
    }

    /**
     * Gets the version of the Mozilla SSL configuration.
     *
     * @return The version as a double.
     */
    public double getVersion() {
        return version;
    }
}
