package de.morihofi.acmeserver.config.proxy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents an HTTP challenge configuration.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class HttpChallenge implements Serializable {
    private Integer port;

    private String host;

    private String type;

    private Boolean enabled;

    private Authentication authentication;

    /**
     * Get the port for the HTTP challenge.
     * @return The port.
     */
    public Integer getPort() {
        return this.port;
    }

    /**
     * Set the port for the HTTP challenge.
     * @param port The port to set.
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Get the host for the HTTP challenge.
     * @return The host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the host for the HTTP challenge.
     * @param host The host to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the type of the HTTP challenge.
     * @return The type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Set the type of the HTTP challenge.
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Check if the HTTP challenge is enabled.
     * @return True if enabled, false otherwise.
     */
    public Boolean getEnabled() {
        return this.enabled;
    }

    /**
     * Set the enabled status of the HTTP challenge.
     * @param enabled The enabled status to set.
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the authentication information for the HTTP challenge.
     * @return The authentication information.
     */
    public Authentication getAuthentication() {
        return this.authentication;
    }

    /**
     * Set the authentication information for the HTTP challenge.
     * @param authentication The authentication information to set.
     */
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * Represents authentication information for an HTTP challenge.
     */
    public static class Authentication implements Serializable {
        private String password;
        private boolean enabled;
        private String username;

        /**
         * Get the password for authentication.
         * @return The password.
         */
        public String getPassword() {
            return this.password;
        }

        /**
         * Set the password for authentication.
         * @param password The password to set.
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Check if authentication is enabled.
         * @return True if enabled, false otherwise.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set the enabled status for authentication.
         * @param enabled The enabled status to set.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Set the enabled status for authentication.
         * @param enabled The enabled status to set.
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Get the username for authentication.
         * @return The username.
         */
        public String getUsername() {
            return this.username;
        }

        /**
         * Set the username for authentication.
         * @param username The username to set.
         */
        public void setUsername(String username) {
            this.username = username;
        }
    }
}
