/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config.network;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.io.Serializable;

/**
 * Represents configuration parameters for a proxy.
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ProxyConfig implements Serializable {
    /**
     * Indicates whether the proxy is enabled.
     */
    private Boolean enabled = false;

    /**
     * The host name or IP address of the proxy.
     */
    private String host = "";

    /**
     * The port number of the proxy.
     */
    private Integer port = 0;

    /**
     * The type of the proxy (http or socks). Any other value except null means direct connection.
     */
    private String type = "";

    /**
     * The authentication configuration for the proxy.
     */
    private Authentication authentication = new Authentication();


    /**
     * Represents authentication information for a proxy.
     */
    @Data
    public static class Authentication implements Serializable {
        /**
         * Indicates whether proxy authentication is enabled.
         */
        private boolean enabled;

        /**
         * The username for proxy authentication.
         */
        private String username;

        /**
         * The password for proxy authentication.
         */
        private String password;

    }
}
