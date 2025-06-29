/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.cryptography.keystore;

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
