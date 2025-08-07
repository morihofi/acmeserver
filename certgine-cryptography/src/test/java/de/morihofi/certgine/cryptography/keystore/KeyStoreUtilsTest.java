/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.keystore;

import de.morihofi.certgine.types.cryptography.CryptoStoreManagerConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreUtilsTest {

    @Test
    @DisplayName("isAllZero detects non zero")
    void testIsAllZero() {
        assertTrue(KeyStoreUtils.isAllZero(new char[]{'\0', '\0'}));
        assertFalse(KeyStoreUtils.isAllZero(new char[]{'a'}));
    }

    @Test
    @DisplayName("alias helpers")
    void testAliasHelpers() {
        String uuid = "123";
        assertEquals(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_TSA + uuid,
                KeyStoreUtils.getKeyStoreAliasForTimestampAuthority(uuid));
        assertEquals(CryptoStoreManagerConstants.KEYSTORE_ALIASPREFIX_INTERMEDIATECA + uuid,
                KeyStoreUtils.getKeyStoreAliasForProvisionerIntermediate(uuid));
    }
}
