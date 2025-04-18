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

package de.morihofi.acmeserver.core.tools.certificate.cryptoops.ksconfig;

import java.nio.file.Path;

/**
 * Represents a configuration for a PKCS#12 keystore, which is a file-based keystore typically stored in a .p12 file. This configuration
 * includes the file path and the password required to access the keystore.
 */
public class PKCS12KeyStoreConfig implements IKeyStoreConfig {

    /**
     * The password required to access the PKCS#12 keystore.
     */
    private String password;

    /**
     * The file system path to the PKCS#12 keystore file (.p12).
     */
    private Path path;

    /**
     * Constructs a new PKCS12KeyStoreConfig with the specified file path and password.
     *
     * @param path     The file system path to the PKCS#12 keystore file.
     * @param password The password required for accessing the keystore.
     */
    public PKCS12KeyStoreConfig(Path path, String password) {
        this.password = password;
        this.path = path;
    }

    /**
     * Retrieves the password required to access the PKCS#12 keystore.
     *
     * @return The password as a String.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password required to access the PKCS#12 keystore.
     *
     * @param password The new password as a String.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Retrieves the file system path to the PKCS#12 keystore file.
     *
     * @return The file path as a Path.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Sets the file system path to the PKCS#12 keystore file.
     *
     * @param path The new file path as a Path.
     */
    public void setPath(Path path) {
        this.path = path;
    }
}
