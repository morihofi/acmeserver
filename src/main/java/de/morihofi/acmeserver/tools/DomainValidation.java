package de.morihofi.acmeserver.tools;

import java.util.regex.Pattern;

public class DomainValidation {
    private static final String DOMAIN_AND_HOSTNAME_PATTERN
            = "^(?!-)([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z0-9-]{2,}$|^[A-Za-z0-9-]{1,63}(?<!-)$";


    private static final Pattern PATTERN = Pattern.compile(DOMAIN_AND_HOSTNAME_PATTERN);

    public static boolean isValidDomain(String domain) {
        if (domain == null) {
            return false;
        }
        return PATTERN.matcher(domain).matches();
    }
}
