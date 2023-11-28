package de.morihofi.acmeserver.tools.regex;

public class ConfigCheck {
    public static boolean isValidProvisionerName(String provisionerName) {
        if (provisionerName == null) {
            return false;
        }
        return provisionerName.matches("^[A-Za-z0-9_-]+$") && provisionerName.length() <= 255;
    }


}
