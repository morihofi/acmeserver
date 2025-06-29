/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.regex;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.regex.Pattern;

/**
 * Utility class for domain and hostname validation. This class provides methods to validate domain and hostname strings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainValidator {

    /**
     * Regular expression pattern for validating domain and hostname strings. This pattern enforces certain rules for domain and hostname
     * format.
     */
    private static final Pattern DOMAIN_AND_HOSTNAME_PATTERN =
            Pattern.compile("^(?!-)([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z0-9-]{2,}$|^[A-Za-z0-9-]{1,63}(?<!-)$");

    /**
     * Validates a domain or hostname string.
     *
     * @param domain        The domain or hostname to validate.
     * @param allowWildcard If true, wildcard domains are allowed (e.g., "*.example.com").
     * @return True if the domain or hostname is valid, otherwise false.
     */
    public static boolean isValidDomain(@NonNull final String domain, boolean allowWildcard) {
        if (IpValidator.isIpAddress(domain)) { // Check if the string is an IP address
            return false;
        }

        String nonWildcardDomain = domain;

        // Check for wildcard domain
        if (allowWildcard && nonWildcardDomain.startsWith("*.")) {

            nonWildcardDomain = domain.substring(2); // Remove wildcard part for validation
        }

        return DOMAIN_AND_HOSTNAME_PATTERN.matcher(nonWildcardDomain).matches();
    }



}
