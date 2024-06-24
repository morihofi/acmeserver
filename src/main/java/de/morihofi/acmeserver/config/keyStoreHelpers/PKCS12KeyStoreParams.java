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

package de.morihofi.acmeserver.config.keyStoreHelpers;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationClassExtends;
import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

/**
 * Represents the parameters for a PKCS12 KeyStore. This class extends {@link KeyStoreParams} to include parameters specific to PKCS12
 * KeyStores, such as the location of the KeyStore file.
 */
@ConfigurationClassExtends(configName = "pkcs12")
public class PKCS12KeyStoreParams extends KeyStoreParams {
    /**
     * Path to KeyStore file
     */
    @ConfigurationField(name = "Path to KeyStore file", required = true)
    private String location;

    /**
     * Constructs a new PKCS12KeyStoreParams object. Sets the type of KeyStore to 'pkcs12'.
     */
    public PKCS12KeyStoreParams() {
        type = "pkcs12";
    }

    /**
     * Retrieves the location of the PKCS12 KeyStore file. The location is a path to the KeyStore file on the filesystem.
     *
     * @return The KeyStore file location as a {@code String}.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location of the PKCS12 KeyStore file.
     *
     * @param location The KeyStore file location as a {@code String}.
     */
    public void setLocation(String location) {
        this.location = location;
    }
}
