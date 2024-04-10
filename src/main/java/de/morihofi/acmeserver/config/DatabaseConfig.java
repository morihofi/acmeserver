package de.morihofi.acmeserver.config;

import java.io.Serializable;

/**
 * Represents configuration parameters for a database connection, including engine, host, user, password, and database name.
 */
public abstract class DatabaseConfig implements Serializable {

    private String user;
    private String password;

    /**
     * Get the username for the database connection.
     * @return The database username.
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the username for the database connection.
     * @param user The database username to set.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the password for the database connection.
     * @return The database password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password for the database connection.
     * @param password The database password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

}
