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
