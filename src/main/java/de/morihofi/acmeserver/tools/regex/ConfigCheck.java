package de.morihofi.acmeserver.tools.regex;

public class ConfigCheck {

    private ConfigCheck(){}
    public static boolean isValidProvisionerName(String provisionerName) {
        if (provisionerName == null) {
            return false;
        }
        return provisionerName.matches("^[a-z0-9_-]+$") && provisionerName.length() <= 255;
    }


}
