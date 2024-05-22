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
