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

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Abstract base class for KeyStore parameters. This class provides a common structure for different types of KeyStore parameters, allowing
 * them to be serialized and managed in a unified way. Subclasses should provide specific implementations and additional properties relevant
 * to the particular type of KeyStore.
 */
public abstract class KeyStoreParams implements Serializable {
    @ConfigurationField(name = "KeyStore Type", required = true, hideVisibility = true)
    protected String type;
    @ConfigurationField(name = "Password", required = true)
    protected String password;

    /**
     * Retrieves the password of the KeyStore. The password is used to access the KeyStore and should be protected.
     *
     * @return The password of the KeyStore as a {@code String}.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for the KeyStore. This method allows specifying a password to access the KeyStore.
     *
     * @param password The password for the KeyStore as a {@code String}.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
