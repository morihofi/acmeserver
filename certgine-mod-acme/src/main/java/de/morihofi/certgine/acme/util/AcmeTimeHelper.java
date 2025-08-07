package de.morihofi.certgine.acme.util;

import lombok.NonNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class AcmeTimeHelper {
    /**
     * Formats an {@link Instant} as a string in the ACME date format.
     *
     * @param instant the {@link Instant} to be formatted
     * @return a string representing the instant in ISO-8601 format with a trailing {@code Z}
     */
    @NonNull
    public static String formatInstantForAcme(@NonNull Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
