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

import java.io.Serializable;

/**
 * Represents configuration for HTTP and HTTPS ports.
 */
public class Ports implements Serializable {
    private int http = 80;
    private int https = 443;

    /**
     * Get the HTTP port number.
     *
     * @return The HTTP port number.
     */
    public int getHttp() {
        return this.http;
    }

    /**
     * Set the HTTP port number.
     *
     * @param http The HTTP port number to set.
     */
    public void setHttp(int http) {
        this.http = http;
    }

    /**
     * Get the HTTPS port number.
     *
     * @return The HTTPS port number.
     */
    public int getHttps() {
        return this.https;
    }

    /**
     * Set the HTTPS port number.
     *
     * @param https The HTTPS port number to set.
     */
    public void setHttps(int https) {
        this.https = https;
    }
}
