/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.regex;

import java.util.regex.Pattern;

/**
 * Utility class for domain and hostname validation. This class provides methods to validate domain and hostname strings.
 */
public class DomainAndIpValidation {

    /**
     * Regular expression pattern for validating domain and hostname strings. This pattern enforces certain rules for domain and hostname
     * format.
     */
    private static final Pattern DOMAIN_AND_HOSTNAME_PATTERN =
            Pattern.compile("^(?!-)([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z0-9-]{2,}$|^[A-Za-z0-9-]{1,63}(?<!-)$");
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
     * /** Validates a domain or hostname string.
     *
     * @param domain        The domain or hostname to validate.
     * @param allowWildcard If true, wildcard domains are allowed (e.g., "*.example.com").
     * @return True if the domain or hostname is valid, otherwise false.
     */
    public static boolean isValidDomain(final String domain, boolean allowWildcard) {
        if (domain == null) {
            return false;
        }
        if (isIpAddress(domain)) { // Check if the string is an IP address
            return false;
        }

        String nonWildcardDomain = domain;

        // Check for wildcard domain
        if (allowWildcard && nonWildcardDomain.startsWith("*.")) {

            nonWildcardDomain = domain.substring(2); // Remove wildcard part for validation
        }

        return DOMAIN_AND_HOSTNAME_PATTERN.matcher(nonWildcardDomain).matches();
    }

    /**
     * Checks if the given string is an IP address.
     *
     * @param input The string to check.
     * @return True if the input is an IP address, otherwise false.
     */
    public static boolean isIpAddress(String input) {
        return isIPv4Address(input) || isIPv6Address(input);
    }

    /**
     * Checks if the given string is an IPv4 address.
     *
     * @param input The string to check.
     * @return True if the input is an IPv4 address, otherwise false.
     */
    public static boolean isIPv4Address(String input) {
        return IPv4PATTERN.matcher(input).matches();
    }

    /**
     * Checks if the given string is an IPv6 address.
     *
     * @param input The string to check.
     * @return True if the input is an IPv6 address, otherwise false.
     */
    public static boolean isIPv6Address(String input) {
        return IPv6PATTERN.matcher(input).matches();
    }

    private DomainAndIpValidation() {
    }
}
