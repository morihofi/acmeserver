/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Represents configuration parameters for an email server, including host, port, encryption, username, password, and enabled status.
 */
public class EmailConfig implements Serializable {
    @ConfigurationField(name = "Enable E-Mail sending")
    private Boolean enabled = false;
    @ConfigurationField(name = "SMTP Host")
    private String host = "";
    @ConfigurationField(name = "SMTP Port")
    private Integer port = 0;
    @ConfigurationField(name = "SMTP Username")
    private String username = "";
    @ConfigurationField(name = "SMTP Password")
    private String password = "";
    @ConfigurationField(name = "SMTP Connection Encryption")
    private String encryption = "none";


    /**
     * Get the password for the email server.
     *
     * @return The email server password.
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Set the password for the email server.
     *
     * @param password The email server password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the encryption type for the email server.
     *
     * @return The encryption type.
     */
    public String getEncryption() {
        return this.encryption;
    }

    /**
     * Set the encryption type for the email server.
     *
     * @param encryption The encryption type to set.
     */
    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    /**
     * Get the port number for the email server.
     *
     * @return The port number.
     */
    public Integer getPort() {
        return this.port;
    }

    /**
     * Set the port number for the email server.
     *
     * @param port The port number to set.
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Get the host address of the email server.
     *
     * @return The email server host address.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the host address of the email server.
     *
     * @param host The email server host address to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Check if email server configuration is enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public Boolean getEnabled() {
        return this.enabled;
    }

    /**
     * Set the enabled status of email server configuration.
     *
     * @param enabled The enabled status to set.
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the username for the email server.
     *
     * @return The email server username.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Set the username for the email server.
     *
     * @param username The email server username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }


}
