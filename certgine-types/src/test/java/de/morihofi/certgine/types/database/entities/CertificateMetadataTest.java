/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.database.entities;

import de.morihofi.certgine.types.database.entities.authority.CertificateMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CertificateMetadata builder")
class CertificateMetadataTest {

    @Test
    @DisplayName("builder accepts valid values")
    void builderValid() {
        assertDoesNotThrow(() -> CertificateMetadata.builder()
                .commonName("test")
                .organisation("Org")
                .email("info@example.com")
                .countryCode("DE")
                .build());
    }

    @Test
    @DisplayName("builder rejects invalid values")
    void builderInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                CertificateMetadata.builder().commonName("").build());
        assertThrows(IllegalArgumentException.class, () ->
                CertificateMetadata.builder().commonName("cn").countryCode("GER").build());
        assertThrows(IllegalArgumentException.class, () ->
                CertificateMetadata.builder().commonName("cn").email("broken@").build());
    }
}
