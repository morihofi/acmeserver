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

package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Represents configuration parameters for a database connection, including engine, host, user, password, and database name.
 */
public abstract class DatabaseConfig implements Serializable {
    @ConfigurationField(name = "Username", required = true)
    private String user;
    @ConfigurationField(name = "Password", required = true)
    private String password;

    /**
     * Get the username for the database connection.
     *
     * @return The database username.
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the username for the database connection.
     *
     * @param user The database username to set.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the password for the database connection.
     *
     * @return The database password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password for the database connection.
     *
     * @param password The database password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
