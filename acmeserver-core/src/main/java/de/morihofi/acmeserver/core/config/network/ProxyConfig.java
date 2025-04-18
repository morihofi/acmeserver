/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.config.network;

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
