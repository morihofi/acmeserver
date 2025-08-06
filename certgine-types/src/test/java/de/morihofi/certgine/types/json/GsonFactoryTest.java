/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.json;

import com.google.gson.Gson;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link GsonFactory} and its timezone aware instant adapter.
 */
class GsonFactoryTest {

    private TimeZone originalTz;

    @BeforeEach
    void setUp() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTz);
    }

    @Test
    void testInstantSerialization() {
        Gson gson = GsonFactory.createGson();
        Instant instant = Instant.parse("2021-03-15T10:15:30Z");
        assertEquals("\"2021-03-15T10:15:30Z\"", gson.toJson(instant));
    }

    @Test
    void testInstantDeserialization() {
        Gson gson = GsonFactory.createGson();
        Instant instant = gson.fromJson("\"2021-03-15T10:15:30Z\"", Instant.class);
        assertEquals(Instant.parse("2021-03-15T10:15:30Z"), instant);
    }


    @Test
    void serializeInstantProducesUtcString() {
        Gson gson = GsonFactory.createGson();
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        String json = gson.toJson(instant);
        assertEquals("\"2023-01-01T00:00:00Z\"", json);
        assertTrue(json.contains("Z"));
    }

    @Test
    void deserializeInstantWithOffsetParsesToUtc() {
        Gson gson = GsonFactory.createGson();
        Instant parsed = gson.fromJson("\"2024-01-01T02:00:00+02:00\"", Instant.class);
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), parsed);
    }

    @Test
    void testRevokedCertificateSerialization() {
        Gson gson = GsonFactory.createGson();
        RevokedCertificate cert = new RevokedCertificate(BigInteger.ONE,
                Instant.parse("2021-03-15T10:15:30Z"), 0);
        String json = gson.toJson(cert);
        assertTrue(json.contains("\"revocationDate\":\"2021-03-15T10:15:30Z\""));
    }

    @Test
    void testRevokedCertificateDeserialization() {
        Gson gson = GsonFactory.createGson();
        String json = "{\"serialNumber\":1,\"revocationDate\":\"2021-03-15T10:15:30Z\",\"revocationReason\":0}";
        RevokedCertificate cert = gson.fromJson(json, RevokedCertificate.class);
        assertEquals(Instant.parse("2021-03-15T10:15:30Z"), cert.revocationDate());
    }

}
