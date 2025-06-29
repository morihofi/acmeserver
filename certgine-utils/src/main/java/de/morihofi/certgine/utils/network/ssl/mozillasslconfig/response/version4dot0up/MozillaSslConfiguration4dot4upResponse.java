/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.ssl.mozillasslconfig.response.version4dot0up;

import lombok.Getter;

/**
 * Represents the response for Mozilla SSL configuration version 4.4 and up.
 * This class contains configuration details, a reference URL, and the version of the configuration.
 */
@Getter
public class MozillaSslConfiguration4dot4upResponse {

    /**
     * Configurations object containing SSL configuration details.
     */
    private Configurations configurations;

    /**
     * A reference URL for the Mozilla SSL configuration.
     */
    private String href;

    /**
     * The version of the Mozilla SSL configuration.
     */
    private long version;
}
