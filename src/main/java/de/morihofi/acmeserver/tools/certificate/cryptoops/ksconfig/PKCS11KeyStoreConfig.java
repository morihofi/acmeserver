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

package de.morihofi.acmeserver.tools.certificate.cryptoops.ksconfig;

import java.nio.file.Path;

/**
 * Represents a configuration for a PKCS#11 keystore, which is a hardware security module (HSM) based keystore. This configuration includes
 * information such as the library path, slot, and PIN required to access the HSM.
 */
public class PKCS11KeyStoreConfig implements IKeyStoreConfig {

    /**
     * The PIN (Personal Identification Number) required to access the PKCS#11 keystore.
     */
    private String pin;

    /**
     * The file system path to the PKCS#11 library used for communication with the hardware security module (HSM).
     */
    private Path libraryPath;

    /**
     * The slot number identifying the specific HSM slot to be used.
     */
    private int slot;

    /**
     * Constructs a new PKCS11KeyStoreConfig with the specified library path, slot, and PIN.
     *
     * @param libraryPath The file system path to the PKCS#11 library.
     * @param slot        The slot number of the HSM.
     * @param pin         The PIN required for accessing the HSM.
     */
    public PKCS11KeyStoreConfig(Path libraryPath, int slot, String pin) {
        this.pin = pin;
        this.libraryPath = libraryPath;
        this.slot = slot;
    }

    /**
     * Retrieves the PIN required to access the PKCS#11 keystore.
     *
     * @return The PIN as a String.
     */
    @Override
    public String getPassword() {
        return pin;
    }

    /**
     * Sets the PIN required to access the PKCS#11 keystore.
     *
     * @param pin The new PIN as a String.
     */
    public void setPassword(String pin) {
        this.pin = pin;
    }

    /**
     * Retrieves the file system path to the PKCS#11 library.
     *
     * @return The library path as a Path.
     */
    public Path getLibraryPath() {
        return libraryPath;
    }

    /**
     * Sets the file system path to the PKCS#11 library.
     *
     * @param libraryPath The new library path as a Path.
     */
    public void setLibraryPath(Path libraryPath) {
        this.libraryPath = libraryPath;
    }

    /**
     * Retrieves the slot number identifying the specific HSM slot.
     *
     * @return The slot number as an integer.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Sets the slot number identifying the specific HSM slot.
     *
     * @param slot The new slot number as an integer.
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }
}
