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

package de.morihofi.acmeserver.types.cryptography.keystore;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

/**
 * Represents a configuration for a PKCS#11 keystore, which is a hardware security module (HSM) based keystore. This configuration includes
 * information such as the library path, slot, and PIN required to access the HSM.
 */
@Data
@AllArgsConstructor
public class PKCS11KeyStoreConfig implements IKeyStoreConfig {

    /**
     * The file system path to the PKCS#11 library used for communication with the hardware security module (HSM).
     */
    private final Path libraryPath;

    /**
     * The slot number identifying the specific HSM slot to be used.
     */
    private final int slot;

    /**
     * The PIN (Personal Identification Number) required to access the PKCS#11 keystore.
     */
    private final char[] pin;


    @Override
    public char[] getPassword() {
        return getPin();
    }
}
