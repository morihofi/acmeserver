/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.api.acme.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Identifier} type resolution and validation.
 */
class IdentifierTest {

    @ParameterizedTest
    @ValueSource(strings = {"DNS", "dns", "DnS"})
    void getTypeAsEnumConstant_resolvesDnsCaseInsensitive(String input) {
        Identifier identifier = new Identifier(input, "example.com");
        assertEquals(Identifier.IDENTIFIER_TYPE.DNS, identifier.getTypeAsEnumConstant());
    }

    @Test
    void getTypeByName_throwsOnUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> new Identifier("UNKNOWN", "example.com"));
    }
}

