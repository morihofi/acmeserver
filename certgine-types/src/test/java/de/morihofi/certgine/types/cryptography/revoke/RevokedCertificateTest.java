/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.cryptography.revoke;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.time.Instant;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RevokedCertificate} ensuring UTC formatting.
 */
class RevokedCertificateTest {

    private TimeZone originalTz;

    @BeforeEach
    void setUp() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTz);
    }

    @Test
    void formattedRevocationDateUsesUtc() {
        Instant instant = Instant.parse("2023-05-01T10:15:30Z");
        RevokedCertificate rc = new RevokedCertificate(BigInteger.ONE, instant, 0);
        assertEquals("2023-05-01T10:15:30Z", rc.formattedRevocationDate());
    }

    @Test
    void formattedRevocationDateKeepsFractionalSeconds() {
        Instant instant = Instant.parse("2023-01-01T00:00:00.123456789Z");
        RevokedCertificate rc = new RevokedCertificate(BigInteger.TWO, instant, 0);
        assertEquals("2023-01-01T00:00:00.123456789Z", rc.formattedRevocationDate());
    }
}
