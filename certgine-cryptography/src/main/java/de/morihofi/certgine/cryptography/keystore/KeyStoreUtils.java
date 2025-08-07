/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.keystore;

import de.morihofi.certgine.types.cryptography.CryptoStoreManagerConstants;
import lombok.NonNull;

/**
 * Utility helpers related to keystore aliases and passwords.
 */
public final class KeyStoreUtils {
    private KeyStoreUtils() {
    }

    /**
     * Clones a password char array.
     *
     * @param password source password
     * @return cloned array or {@code null} if input is {@code null}
     */
    public static char[] clonePassword(char[] password) {
        return password == null ? null : password.clone();
    }

    /**
     * Checks if all characters of the array are zero.
     *
     * @param array char array to check
     * @return {@code true} if all characters are zero
     */
    public static boolean isAllZero(char[] array) {
        if (array == null) {
            return true;
        }
        for (char c : array) {
            if (c != '\0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the keystore alias for a timestamp authority.
     *
     * @param uuid internal UUID of the TSA
     * @return keystore alias
     */
    @NonNull
    public static String getKeyStoreAliasForTimestampAuthority(@NonNull String uuid) {
        return CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA + uuid;
    }

    /**
     * Builds the keystore alias for a provisioner's intermediate certificate.
     *
     * @param uuid internal UUID of the provisioner
     * @return keystore alias
     */
    @NonNull
    public static String getKeyStoreAliasForProvisionerIntermediate(@NonNull String uuid) {
        return CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid;
    }
}
