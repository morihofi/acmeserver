package de.morihofi.certgine.utils.datetime;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeToolsTest {

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
