package de.morihofi.acmeserver.postsetup.inputcheck;

import de.morihofi.acmeserver.tools.regex.DomainValidation;

public class FQDNInputChecker implements InputChecker {
    @Override
    public boolean isValid(String input) {
        return DomainValidation.isValidDomain(input, false);
    }
}
