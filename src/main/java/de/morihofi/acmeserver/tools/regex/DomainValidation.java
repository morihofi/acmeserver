package de.morihofi.acmeserver.tools.regex;

import java.util.regex.Pattern;

public class DomainValidation {
    private static final String DOMAIN_AND_HOSTNAME_PATTERN
            = "^(?!-)([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z0-9-]{2,}$|^[A-Za-z0-9-]{1,63}(?<!-)$";


    private static final Pattern PATTERN = Pattern.compile(DOMAIN_AND_HOSTNAME_PATTERN);

    /**
     * Validates a domain or hostname string.
     *
     * @param domain        The domain or hostname to validate.
     * @param allowWildcard If true, wildcard domains are allowed (e.g., "*.example.com").
     * @return True if the domain or hostname is valid, otherwise false.
     */
    public static boolean isValidDomain(final String domain, boolean allowWildcard) {
        if (domain == null) {
            return false;
        }
        String nonWildcardDomain = domain;

        if (allowWildcard) {
            // Check for wildcard domain
            if (nonWildcardDomain.startsWith("*.")) {
                nonWildcardDomain = domain.substring(2); // Remove wildcard part for validation
            }
        }

        return PATTERN.matcher(nonWildcardDomain).matches();
    }
}
