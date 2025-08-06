/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.datetime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TimeTools} verifying correct timezone handling.
 */
class TimeToolsTest {

    private TimeZone originalTz;

    @BeforeEach
    void setUp() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTz);
    }

    @Test
    void formatInstantForAcmeAlwaysReturnsUtc() {
        Instant instant = Instant.parse("2024-08-20T10:15:30Z");
        String formatted = TimeTools.formatInstantForAcme(instant);
        assertEquals("2024-08-20T10:15:30Z", formatted);
        assertTrue(formatted.endsWith("Z"));
    }

    @Test
    void testFormatInstantForAcme() {
        Instant instant = Instant.parse("2021-03-15T10:15:30Z");
        assertEquals("2021-03-15T10:15:30Z", TimeTools.formatInstantForAcme(instant));
    }

    @Test
    void testAddToInstant() {
        Instant start = Instant.parse("2020-02-28T00:00:00Z");
        Instant result = TimeTools.addToInstant(start, 1, 0, 1);
        assertEquals(Instant.parse("2021-03-01T00:00:00Z"), result);
    }

    @Test
    void addToInstantHandlesDstTransitions() {
        Instant beforeDst = Instant.parse("2021-03-27T12:00:00Z");
        Instant result = TimeTools.addToInstant(beforeDst, 0, 0, 1);
        assertEquals(Instant.parse("2021-03-28T12:00:00Z"), result);
    }

    @Test
    void addToInstantHandlesLeapDays() {
        Instant leap = Instant.parse("2024-02-28T00:00:00Z");
        Instant result = TimeTools.addToInstant(leap, 0, 0, 1);
        assertEquals(Instant.parse("2024-02-29T00:00:00Z"), result);
    }

    @Test
    void addToInstantSupportsNegativeValues() {
        Instant start = Instant.parse("2023-01-15T00:00:00Z");
        Instant result = TimeTools.addToInstant(start, 0, -1, -10);
        assertEquals(Instant.parse("2022-12-05T00:00:00Z"), result);
    }

    @Test
    void addToInstantWorksAcrossYears() {
        Instant start = Instant.parse("2023-12-31T00:00:00Z");
        Instant result = TimeTools.addToInstant(start, 0, 0, 1);
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), result);
    }

    @Test
    void makeInstantForOutliveIntermediateCertificateChoosesEarlier() {
        Instant intermediate = Instant.parse("2024-01-01T00:00:00Z");
        Instant proposed = Instant.parse("2025-01-01T00:00:00Z");
        assertEquals(intermediate,
                TimeTools.makeInstantForOutliveIntermediateCertificate(intermediate, proposed));
    }

    @Test
    void makeInstantForOutliveIntermediateCertificateKeepsProposedWhenEarlier() {
        Instant intermediate = Instant.parse("2024-01-01T00:00:00Z");
        Instant proposed = Instant.parse("2023-12-31T00:00:00Z");
        assertEquals(proposed,
                TimeTools.makeInstantForOutliveIntermediateCertificate(intermediate, proposed));
    }

    @Test
    void testMakeInstantForOutliveIntermediateCertificate() {
        Instant intermediate = Instant.parse("2024-01-01T00:00:00Z");
        Instant proposedBefore = Instant.parse("2023-12-31T00:00:00Z");
        Instant proposedAfter = Instant.parse("2024-06-01T00:00:00Z");
        assertEquals(proposedBefore,
                TimeTools.makeInstantForOutliveIntermediateCertificate(intermediate, proposedBefore));
        assertEquals(intermediate,
                TimeTools.makeInstantForOutliveIntermediateCertificate(intermediate, proposedAfter));
    }

    @Test
    void testFormatInstantForAcmeAlwaysUtc() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        try {
            Instant instant = Instant.parse("2021-03-15T10:15:30Z");
            assertEquals("2021-03-15T10:15:30Z", TimeTools.formatInstantForAcme(instant));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void testAddToInstantIgnoresDefaultTimeZone() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        try {
            Instant start = Instant.parse("2023-03-26T00:00:00Z");
            Instant result = TimeTools.addToInstant(start, 0, 0, 1);
            assertEquals(Instant.parse("2023-03-27T00:00:00Z"), result);
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
