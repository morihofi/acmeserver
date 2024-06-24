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
package de.morihofi.acmeserver.api.provisioner.statistics.responses;

/**
 * Represents a response entry for a provisioner listing.
 *
 * <p>This class contains the details of a provisioner, including its name and directory URL,
 * which are provided as part of the response when listing all available provisioners.</p>
 */
public class ProvisionerListEntryResponse {
    /**
     * The name of the provisioner.
     */
    private String name;

    /**
     * The directory URL of the provisioner.
     */
    private String directoryUrl;

    /**
     * Gets the name of the provisioner.
     *
     * @return The name of the provisioner.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the provisioner.
     *
     * @param name The name to set for the provisioner.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the directory URL of the provisioner.
     *
     * @return The directory URL of the provisioner.
     */
    public String getDirectoryUrl() {
        return directoryUrl;
    }

    /**
     * Sets the directory URL of the provisioner.
     *
     * @param directoryUrl The directory URL to set for the provisioner.
     */
    public void setDirectoryUrl(String directoryUrl) {
        this.directoryUrl = directoryUrl;
    }
}
