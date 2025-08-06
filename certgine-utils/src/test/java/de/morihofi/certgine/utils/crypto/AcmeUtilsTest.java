package de.morihofi.certgine.utils.crypto;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcmeUtilsTest {

    @Test
    void testParseTimestampUtc() {
        Instant expected = Instant.parse("2023-07-20T12:34:56Z");
        assertEquals(expected, AcmeUtils.parseTimestamp("2023-07-20T12:34:56Z"));
    }

    @Test
    void testParseTimestampWithFraction() {
        Instant expected = Instant.parse("2023-07-20T12:34:56.789Z");
        assertEquals(expected, AcmeUtils.parseTimestamp("2023-07-20T12:34:56.789Z"));
    }

    @Test
    void testParseTimestampWithPositiveOffset() {
        Instant expected = OffsetDateTime.parse("2023-07-20T12:34:56+02:00").toInstant();
        assertEquals(expected, AcmeUtils.parseTimestamp("2023-07-20T12:34:56+02:00"));
    }

    @Test
    void testParseTimestampWithNegativeOffsetNoColon() {
        Instant expected = OffsetDateTime.parse("2023-07-20T12:34:56-02:30").toInstant();
        assertEquals(expected, AcmeUtils.parseTimestamp("2023-07-20T12:34:56-0230"));
    }

    @Test
    void testParseTimestampInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> AcmeUtils.parseTimestamp("2023-07-20 12:34:56Z"));
    }

    @Test
    void testParseTimestampInvalidTimezone() {
        assertThrows(DateTimeException.class,
                () -> AcmeUtils.parseTimestamp("2023-07-20T12:34:56+25:00"));
    }
}
