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
 * Represents metadata configuration for a system, including website URL and terms of service (TOS) information.
 */
public class MetadataConfig implements Serializable {
    private String website;
    private String tos;

    /**
     * Get the URL of the system's website.
     *
     * @return The website URL.
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Set the URL of the system's website.
     *
     * @param website The website URL to set.
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * Get the terms of service (TOS) information for the system.
     *
     * @return The TOS information.
     */
    public String getTos() {
        return tos;
    }

    /**
     * Set the terms of service (TOS) information for the system.
     *
     * @param tos The TOS information to set.
     */
    public void setTos(String tos) {
        this.tos = tos;
    }
}
