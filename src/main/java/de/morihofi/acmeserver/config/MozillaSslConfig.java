package de.morihofi.acmeserver.config;

import java.io.Serializable;

public class MozillaSslConfig implements Serializable {
    private boolean enabled = false;
    private String version = "5.7";
    private String configuration = "intermediate";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
