package de.morihofi.acmeserver.postsetup.inputcheck;

import de.morihofi.acmeserver.tools.regex.DomainValidation;

/**
 * A utility class for checking the validity of Fully Qualified Domain Name (FQDN) input strings.
 * It implements the {@link InputChecker} functional interface to provide custom input validation.
 */
public class FQDNInputChecker implements InputChecker {

    /**
     * Checks whether the given input string is a valid Fully Qualified Domain Name (FQDN).
     *
     * @param input The input string to be checked for validity as an FQDN.
     * @return {@code true} if the input is a valid FQDN, {@code false} otherwise.
     */
    @Override
    public boolean isValid(String input) {
        return DomainValidation.isValidDomain(input, false);
    }
}
