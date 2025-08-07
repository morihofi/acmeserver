/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.datetime;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class offering common time related helpers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeTools {

    private static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Adds the specified number of years, months and days to the given instant.
     * Calculations are performed using {@link ZoneId#of(String)} with {@code UTC}.
     *
     * @param start  the starting instant
     * @param years  years to add (may be negative)
     * @param months months to add (may be negative)
     * @param days   days to add (may be negative)
     * @return the resulting instant after the addition
     */
    @NonNull
    public static Instant addToInstant(@NonNull Instant start, int years, int months, int days) {
        ZonedDateTime zdt = start.atZone(UTC);
        return zdt.plus(Period.of(years, months, days)).toInstant();
    }

    /**
     * Ensures that a proposed server certificate end date does not exceed the
     * expiration date of the intermediate certificate.
     *
     * @param intermediateNotAfter expiration of the intermediate certificate
     * @param proposedEndDate      proposed end date for the server certificate
     * @return the earlier of {@code proposedEndDate} and {@code intermediateNotAfter}
     */
    @NonNull
    public static Instant makeInstantForOutliveIntermediateCertificate(
            @NonNull Instant intermediateNotAfter,
            @NonNull Instant proposedEndDate) {
        return proposedEndDate.isBefore(intermediateNotAfter) ? proposedEndDate : intermediateNotAfter;
    }
}

