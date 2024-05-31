/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.config.proxy;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents an HTTP challenge configuration.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class HttpChallenge implements Serializable {
    @ConfigurationField(name = "Enable Proxy")
    private Boolean enabled;
    @ConfigurationField(name = "Proxy Host")
    private String host;
    @ConfigurationField(name = "Proxy Port")
    private Integer port;
    @ConfigurationField(name = "Proxy Type")
    private String type;
    @ConfigurationField(name = "Proxy Authentication")
    private Authentication authentication;

    /**
     * Get the port for the HTTP challenge.
     *
     * @return The port.
     */
    public Integer getPort() {
        return this.port;
    }

    /**
     * Set the port for the HTTP challenge.
     *
     * @param port The port to set.
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Get the host for the HTTP challenge.
     *
     * @return The host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the host for the HTTP challenge.
     *
     * @param host The host to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the type of the HTTP challenge.
     *
     * @return The type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Set the type of the HTTP challenge.
     *
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Check if the HTTP challenge is enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public Boolean getEnabled() {
        return this.enabled;
    }

    /**
     * Set the enabled status of the HTTP challenge.
     *
     * @param enabled The enabled status to set.
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the authentication information for the HTTP challenge.
     *
     * @return The authentication information.
     */
    public Authentication getAuthentication() {
        return this.authentication;
    }

    /**
     * Set the authentication information for the HTTP challenge.
     *
     * @param authentication The authentication information to set.
     */
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * Represents authentication information for an HTTP challenge.
     */
    public static class Authentication implements Serializable {
        @ConfigurationField(name = "Enable Proxy Authentication")
        private boolean enabled;
        @ConfigurationField(name = "Username")
        private String username;
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
         * Set the enabled status for authentication.
         *
         * @param enabled The enabled status to set.
         */
        public void setEnabled(Boolean enabled) {
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
