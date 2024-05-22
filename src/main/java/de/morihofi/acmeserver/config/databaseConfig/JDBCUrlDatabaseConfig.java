package de.morihofi.acmeserver.config.databaseConfig;

import de.morihofi.acmeserver.config.DatabaseConfig;

public class JDBCUrlDatabaseConfig extends DatabaseConfig {
    private String jdbcUrl;

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }
}
