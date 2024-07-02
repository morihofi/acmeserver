/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.config.network;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents configuration parameters for a proxy.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ProxyConfig implements Serializable {
    /**
     * Indicates whether the proxy is enabled.
     */
    @ConfigurationField(name = "Enable Proxy")
    private Boolean enabled = false;

    /**
     * The host name or IP address of the proxy.
     */
    @ConfigurationField(name = "Proxy Host")
    private String host = "";

    /**
     * The port number of the proxy.
     */
    @ConfigurationField(name = "Proxy Port")
    private Integer port = 0;

    /**
     * The type of the proxy (http or socks). Any other value except null means direct connection.
     */
    @ConfigurationField(name = "Proxy Type")
    private String type = "";

    /**
     * The authentication configuration for the proxy.
     */
    @ConfigurationField(name = "Proxy Authentication")
    private Authentication authentication = new Authentication();

    /**
     * Get the port for the proxy.
     *
     * @return The port.
     */
    public Integer getPort() {
        return this.port;
    }

    /**
     * Set the port for the proxy.
     *
     * @param port The port to set.
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Get the host for the proxy.
     *
     * @return The host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the host for the proxy.
     *
     * @param host The host to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the type of the proxy.
     *
     * @return The type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Set the type of the proxy.
     *
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Check if the proxy is enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public Boolean getEnabled() {
        return this.enabled;
    }

    /**
     * Set the enabled status of the proxy.
     *
     * @param enabled The enabled status to set.
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the authentication information for the proxy.
     *
     * @return The authentication information.
     */
    public Authentication getAuthentication() {
        return this.authentication;
    }

    /**
     * Set the authentication information for the proxy.
     *
     * @param authentication The authentication information to set.
     */
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * Represents authentication information for a proxy.
     */
    public static class Authentication implements Serializable {
        /**
         * Indicates whether proxy authentication is enabled.
         */
        @ConfigurationField(name = "Enable Proxy Authentication")
        private boolean enabled;

        /**
         * The username for proxy authentication.
         */
        @ConfigurationField(name = "Username")
        private String username;

        /**
         * The password for proxy authentication.
         */
        @ConfigurationField(name = "Password")
        private String password;

        /**
         * Get the password for authentication.
         *
         * @return The password.
         */
        public String getPassword() {
            return this.password;
        }

        /**
         * Set the password for authentication.
         *
         * @param password The password to set.
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Check if authentication is enabled.
         *
         * @return True if enabled, false otherwise.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set the enabled status for authentication.
         *
         * @param enabled The enabled status to set.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Get the username for authentication.
         *
         * @return The username.
         */
        public String getUsername() {
            return this.username;
        }

        /**
         * Set the username for authentication.
         *
         * @param username The username to set.
         */
        public void setUsername(String username) {
            this.username = username;
        }
    }
}
