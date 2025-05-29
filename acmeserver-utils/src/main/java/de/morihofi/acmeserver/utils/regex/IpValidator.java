package de.morihofi.acmeserver.utils.regex;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IpValidator {

    // Define the IPv4 pattern
    private static final Pattern IPv4PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    // Define the IPv6 pattern
    private static final Pattern IPv6PATTERN = Pattern.compile(
            "^(([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4})" + // IPv6 normal form
                    "|(([0-9A-Fa-f]{1,4}:){1,7}:)" + // IPv6 shortened at the end
                    "|(([0-9A-Fa-f]{1,4}:){1,6}:[0-9A-Fa-f]{1,4})" + // IPv6 with a shortened block
                    "|(([0-9A-Fa-f]{1,4}:){1,5}(:[0-9A-Fa-f]{1,4}){1,2})" + // and so on
                    "|(([0-9A-Fa-f]{1,4}:){1,4}(:[0-9A-Fa-f]{1,4}){1,3})" +
                    "|(([0-9A-Fa-f]{1,4}:){1,3}(:[0-9A-Fa-f]{1,4}){1,4})" +
                    "|(([0-9A-Fa-f]{1,4}:){1,2}(:[0-9A-Fa-f]{1,4}){1,5})" +
                    "|([0-9A-Fa-f]{1,4}:((:[0-9A-Fa-f]{1,4}){1,6}))" +
                    "|(:((:[0-9A-Fa-f]{1,4}){1,7}|:))" + // IPv6 completely shortened
                    "|([0-9A-Fa-f]{1,4}::([0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})" + // IPv6 with initial shortening
                    "|(::([0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})" + // Further variants of shortening
                    "|([0-9A-Fa-f]{1,4}:){6}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)" // IPv6 with embedded IPv4 address
    );

    /**
     * Checks if the given string is an IP address.
     *
     * @param input The string to check.
     * @return True if the input is an IP address, otherwise false.
     */
    public static boolean isIpAddress(@NonNull String input) {
        return isIPv4Address(input) || isIPv6Address(input);
    }

    /**
     * Checks if the given string is an IPv4 address.
     *
     * @param input The string to check.
     * @return True if the input is an IPv4 address, otherwise false.
     */
    public static boolean isIPv4Address(@NonNull String input) {
        return IPv4PATTERN.matcher(input).matches();
    }

    /**
     * Checks if the given string is an IPv6 address.
     *
     * @param input The string to check.
     * @return True if the input is an IPv6 address, otherwise false.
     */
    public static boolean isIPv6Address(@NonNull String input) {
        return IPv6PATTERN.matcher(input).matches();
    }

}
