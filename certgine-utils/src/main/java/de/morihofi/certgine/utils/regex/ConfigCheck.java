/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.regex;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Utility class for configuration-related checks and validations. This class provides methods to perform various checks on configuration
 * parameters.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigCheck {

    /**
     * Validates a provisioner name to ensure it meets certain criteria.
     *
     * @param provisionerName The provisioner name to validate.
     * @return true if the provisioner name is valid; false otherwise.
     */
    public static boolean isValidProvisionerName(@NonNull String provisionerName) {
        return provisionerName.matches("^[a-z0-9_-]+$") && provisionerName.length() <= 255;
    }

}
