package de.morihofi.acmeserver.config;

public class SslServerConfig {
    private boolean allowLegacyResumption = false;

    public boolean isAllowLegacyResumption() {
        return allowLegacyResumption;
    }

    public void setAllowLegacyResumption(boolean allowLegacyResumption) {
        this.allowLegacyResumption = allowLegacyResumption;
    }
}
