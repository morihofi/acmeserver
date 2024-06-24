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
 * Represents the parameters for a PKCS11 KeyStore. This class extends {@link KeyStoreParams} to include parameters specific to PKCS11
 * KeyStores, such as the library location and slot number.
 */
@ConfigurationClassExtends(configName = "pkcs11")
public class PKCS11KeyStoreParams extends KeyStoreParams {
    /**
     * PKCS#11 Library location
     */
    @ConfigurationField(name = "Library location", required = true)
    private String libraryLocation;
    /**
     * Slot
     */
    @ConfigurationField(name = "Slot", required = true)
    private int slot;

    /**
     * Constructs a new PKCS11KeyStoreParams object. Sets the type of KeyStore to 'pkcs11'.
     */
    public PKCS11KeyStoreParams() {
        type = "pkcs11";
    }

    /**
     * Retrieves the library location for the PKCS11 KeyStore. The library location refers to the path of the PKCS11 library on the system.
     *
     * @return The library location as a {@code String}.
     */
    public String getLibraryLocation() {
        return libraryLocation;
    }

    /**
     * Sets the library location for the PKCS11 KeyStore.
     *
     * @param libraryLocation The library location as a {@code String}.
     */
    public void setLibraryLocation(String libraryLocation) {
        this.libraryLocation = libraryLocation;
    }

    /**
     * Retrieves the slot number used by the PKCS11 KeyStore. The slot number specifies the slot of the PKCS11 token.
     *
     * @return The slot number as an {@code int}.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Sets the slot number for the PKCS11 KeyStore.
     *
     * @param slot The slot number as an {@code int}.
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }
}
