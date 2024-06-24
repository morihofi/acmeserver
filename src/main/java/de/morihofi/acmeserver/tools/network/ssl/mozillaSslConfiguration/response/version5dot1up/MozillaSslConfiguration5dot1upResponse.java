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

package de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version5dot1up;

/**
 * Represents the response for Mozilla SSL configuration version 5.1 and up.
 */
public class MozillaSslConfiguration5dot1upResponse {

    /**
     * The configurations for the SSL settings.
     */
    private Configurations configurations;

    /**
     * The URL reference for more information about the SSL configurations.
     */
    private String href;

    /**
     * The version of the Mozilla SSL configuration.
     */
    private double version;

    /**
     * Gets the SSL configurations.
     *
     * @return The configurations object containing SSL settings.
     */
    public Configurations getConfigurations() {
        return configurations;
    }

    /**
     * Gets the URL reference for more information about the SSL configurations.
     *
     * @return The URL as a string.
     */
    public String getHref() {
        return href;
    }

    /**
     * Gets the version of the Mozilla SSL configuration.
     *
     * @return The version as a double.
     */
    public double getVersion() {
        return version;
    }
}
