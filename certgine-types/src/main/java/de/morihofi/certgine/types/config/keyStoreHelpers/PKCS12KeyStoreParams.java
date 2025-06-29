/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config.keyStoreHelpers;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the parameters for a PKCS12 KeyStore. This class extends {@link KeyStoreParams} to include parameters specific to PKCS12
 * KeyStores, such as the location of the KeyStore file.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PKCS12KeyStoreParams extends KeyStoreParams {
    /**
     * Path to KeyStore file
     */
    private String location;

    /**
     * Constructs a new PKCS12KeyStoreParams object. Sets the type of KeyStore to 'pkcs12'.
     */
    public PKCS12KeyStoreParams() {
        type = "pkcs12";
    }
}
