/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.http;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ProxyScheme}.
 */
class ProxySchemeTest {

    @Test
    void testFromString() {
        assertEquals(Optional.of(ProxyScheme.HTTP), ProxyScheme.fromString("http"));
        assertEquals(Optional.of(ProxyScheme.SOCKS), ProxyScheme.fromString("socks"));
        assertEquals(Optional.of(ProxyScheme.SOCKS), ProxyScheme.fromString("socks5"));
        assertTrue(ProxyScheme.fromString("unknown").isEmpty());
        assertTrue(ProxyScheme.fromString(null).isEmpty());
    }
}
