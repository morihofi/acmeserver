/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.morihofi.certgine.acme.types.api.dns.AcmeOrderIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link AcmeOrderIdentifier} type resolution and validation.
 */
class IdentifierTest {

    @ParameterizedTest
    @ValueSource(strings = {"DNS", "dns", "DnS"})
    void getTypeAsEnumConstant_resolvesDnsCaseInsensitive(String input) {
        AcmeOrderIdentifier identifier = new AcmeOrderIdentifier(input, "example.com");
        assertEquals(AcmeOrderIdentifier.IDENTIFIER_TYPE.DNS, identifier.getTypeAsEnumConstant());
    }

    @Test
    void getTypeByName_throwsOnUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> new AcmeOrderIdentifier("UNKNOWN", "example.com"));
    }
}

