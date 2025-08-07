package de.morihofi.certgine.acme.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class AcmeTimeHelperTest {
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
        String formatted = AcmeTimeHelper.formatInstantForAcme(instant);
        assertEquals("2024-08-20T10:15:30Z", formatted);
        assertTrue(formatted.endsWith("Z"));
    }

    @Test
    void testFormatInstantForAcme() {
        Instant instant = Instant.parse("2021-03-15T10:15:30Z");
        assertEquals("2021-03-15T10:15:30Z", AcmeTimeHelper.formatInstantForAcme(instant));
    }

    @Test
    void testFormatInstantForAcmeAlwaysUtc() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        try {
            Instant instant = Instant.parse("2021-03-15T10:15:30Z");
            assertEquals("2021-03-15T10:15:30Z", AcmeTimeHelper.formatInstantForAcme(instant));
        } finally {
            TimeZone.setDefault(original);
        }
    }
}