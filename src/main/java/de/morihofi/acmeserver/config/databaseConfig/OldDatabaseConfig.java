package de.morihofi.acmeserver.config.databaseConfig;

import de.morihofi.acmeserver.config.DatabaseConfig;

public class OldDatabaseConfig extends DatabaseConfig {
    private String host;
    private String name;

    private String engine;

    /**
     * Get the database engine type.
     *
     * @return The database engine.
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Set the database engine type.
     *
     * @param engine The database engine to set.
     */
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Get the host address of the database server.
     *
     * @return The database host address.
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host address of the database server.
     *
     * @param host The database host address to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the name of the database.
     *
     * @return The database name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the database.
     *
     * @param name The database name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
}
