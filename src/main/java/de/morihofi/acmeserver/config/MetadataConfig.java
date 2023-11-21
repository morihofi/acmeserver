package de.morihofi.acmeserver.config;

import java.io.Serializable;

public class MetadataConfig implements Serializable {
    private String website;
    private String tos;

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTos() {
        return tos;
    }

    public void setTos(String tos) {
        this.tos = tos;
    }
}
