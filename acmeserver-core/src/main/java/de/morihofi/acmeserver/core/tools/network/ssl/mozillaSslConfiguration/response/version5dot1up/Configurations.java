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

package de.morihofi.acmeserver.core.tools.network.ssl.mozillaSslConfiguration.response.version5dot1up;

/**
 * Represents the configurations for SSL settings including modern, old, and intermediate configurations.
 */
public class Configurations {

    /**
     * The modern SSL configuration.
     */
    private Configuration modern;

    /**
     * The old SSL configuration.
     */
    private Configuration old;

    /**
     * The intermediate SSL configuration.
     */
    private Configuration intermediate;

    /**
     * Gets the modern SSL configuration.
     *
     * @return The modern {@link Configuration} object.
     */
    public Configuration getModern() {
        return modern;
    }

    /**
     * Gets the old SSL configuration.
     *
     * @return The old {@link Configuration} object.
     */
    public Configuration getOld() {
        return old;
    }

    /**
     * Gets the intermediate SSL configuration.
     *
     * @return The intermediate {@link Configuration} object.
     */
    public Configuration getIntermediate() {
        return intermediate;
    }
}
