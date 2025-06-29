/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.cryptography.keystore;

/**
 * Interface of an KeyStore configuration
 */
public interface IKeyStoreConfig {
    /**
     * Gets the password for the keystore configuration.
     *
     * @return The keystore password as a String.
     */
    char[] getPassword();
}
