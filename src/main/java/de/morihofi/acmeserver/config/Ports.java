package de.morihofi.acmeserver.config;

import java.io.Serializable;

/**
 * Represents configuration for HTTP and HTTPS ports.
 */
public class Ports implements Serializable {
    private int http = 80;
    private int https = 443;

    /**
     * Get the HTTP port number.
     * @return The HTTP port number.
     */
    public int getHttp() {
        return this.http;
    }

    /**
     * Set the HTTP port number.
     * @param http The HTTP port number to set.
     */
    public void setHttp(int http) {
        this.http = http;
    }

    /**
     * Get the HTTPS port number.
     * @return The HTTPS port number.
     */
    public int getHttps() {
        return this.https;
    }

    /**
     * Set the HTTPS port number.
     * @param https The HTTPS port number to set.
     */
    public void setHttps(int https) {
        this.https = https;
    }
}
