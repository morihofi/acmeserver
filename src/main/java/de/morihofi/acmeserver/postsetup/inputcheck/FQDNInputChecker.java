/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.postsetup.inputcheck;

import de.morihofi.acmeserver.tools.regex.DomainAndIpValidation;

/**
 * A utility class for checking the validity of Fully Qualified Domain Name (FQDN) input strings. It implements the {@link InputChecker}
 * functional interface to provide custom input validation.
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
        return DomainAndIpValidation.isValidDomain(input, false);
    }
}
