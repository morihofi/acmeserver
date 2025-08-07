/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.keystore;

import de.morihofi.certgine.types.cryptography.keystore.PKCS11KeyStoreConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class Pkcs11KeyStoreLoaderTest {

    @Test
    @DisplayName("load throws for invalid library")
    void testLoadPkcs11() {
        PKCS11KeyStoreConfig cfg = new PKCS11KeyStoreConfig(
                java.nio.file.Paths.get("/nonexistent"), 0, "1234".toCharArray());
        Pkcs11KeyStoreLoader loader = new Pkcs11KeyStoreLoader(cfg);
        assertThrows(Throwable.class, loader::load);
    }
}
