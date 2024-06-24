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

package de.morihofi.acmeserver.api.serverInfo.objects;

/**
 * Represents the response structure for a provisioner. This class encapsulates details about a provisioner, primarily its name.
 */
public class ProvisionerResponse {

    /**
     * Provisioner name
     */
    private String name;

    /**
     * Retrieves the name of the provisioner. This name identifies the provisioner within the system.
     *
     * @return The name of the provisioner as a {@code String}.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the provisioner. This method allows assigning or changing the name of the provisioner.
     *
     * @param name The new name for the provisioner as a {@code String}.
     */
    public void setName(String name) {
        this.name = name;
    }
}
