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

package de.morihofi.acmeserver.tools.regex;

/**
 * Utility class for configuration-related checks and validations. This class provides methods to perform various checks on configuration
 * parameters.
 */
public class ConfigCheck {

    /**
     * Validates a provisioner name to ensure it meets certain criteria.
     *
     * @param provisionerName The provisioner name to validate.
     * @return true if the provisioner name is valid; false otherwise.
     */
    public static boolean isValidProvisionerName(String provisionerName) {
        if (provisionerName == null) {
            return false;
        }
        return provisionerName.matches("^[a-z0-9_-]+$") && provisionerName.length() <= 255;
    }

    private ConfigCheck() {}
}
