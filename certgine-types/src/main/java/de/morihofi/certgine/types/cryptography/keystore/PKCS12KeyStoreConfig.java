/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.cryptography.keystore;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

/**
 * Represents a configuration for a PKCS#12 keystore, which is a file-based keystore typically stored in a .p12 file. This configuration
 * includes the file path and the password required to access the keystore.
 */
@Data
@AllArgsConstructor
public class PKCS12KeyStoreConfig implements IKeyStoreConfig {

    /**
     * The file system path to the PKCS#12 keystore file (.p12).
     */
    private Path path;

    /**
     * The password required to access the PKCS#12 keystore.
     */
    private char[] password;
}
