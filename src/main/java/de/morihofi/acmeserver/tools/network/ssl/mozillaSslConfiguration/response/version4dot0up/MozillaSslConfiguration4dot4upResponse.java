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

package de.morihofi.acmeserver.tools.network.ssl.mozillaSslConfiguration.response.version4dot0up;

/**
 * Represents the response for Mozilla SSL configuration version 4.4 and up.
 * This class contains configuration details, a reference URL, and the version of the configuration.
 */
public class MozillaSslConfiguration4dot4upResponse {

    /**
     * Configurations object containing SSL configuration details.
     */
    private Configurations configurations;

    /**
     * A reference URL for the Mozilla SSL configuration.
     */
    private String href;

    /**
     * The version of the Mozilla SSL configuration.
     */
    private long version;

    /**
     * Gets the SSL configurations.
     *
     * @return the Configurations object containing SSL configuration details.
     */
    public Configurations getConfigurations() {
        return configurations;
    }

    /**
     * Gets the reference URL for the Mozilla SSL configuration.
     *
     * @return the reference URL as a String.
     */
    public String getHref() {
        return href;
    }

    /**
     * Gets the version of the Mozilla SSL configuration.
     *
     * @return the version as a long.
     */
    public long getVersion() {
        return version;
    }
}
