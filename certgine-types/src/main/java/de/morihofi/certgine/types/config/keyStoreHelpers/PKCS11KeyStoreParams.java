/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config.keyStoreHelpers;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the parameters for a PKCS11 KeyStore. This class extends {@link KeyStoreParams} to include parameters specific to PKCS11
 * KeyStores, such as the library location and slot number.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PKCS11KeyStoreParams extends KeyStoreParams {
    /**
     * PKCS#11 Library location
     */
    private String libraryLocation;
    /**
     * Slot
     */
    private int slot;

    /**
     * Constructs a new PKCS11KeyStoreParams object. Sets the type of KeyStore to 'pkcs11'.
     */
    public PKCS11KeyStoreParams() {
        type = "pkcs11";
    }

}
