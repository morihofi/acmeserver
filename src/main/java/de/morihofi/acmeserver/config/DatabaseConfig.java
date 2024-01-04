package de.morihofi.acmeserver.config;

import java.io.Serializable;

/**
 * Represents configuration parameters for a database connection, including engine, host, user, password, and database name.
 */
public class DatabaseConfig implements Serializable {
    private String engine;
    private String host;
    private String user;
    private String password;
    private String name;

    /**
     * Get the database engine type.
     * @return The database engine.
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Set the database engine type.
     * @param engine The database engine to set.
     */
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Get the host address of the database server.
     * @return The database host address.
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host address of the database server.
     * @param host The database host address to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

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

    /**
     * Get the name of the database.
     * @return The database name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the database.
     * @param name The database name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
}
