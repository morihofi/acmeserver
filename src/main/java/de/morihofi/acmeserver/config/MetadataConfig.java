package de.morihofi.acmeserver.config;

import java.io.Serializable;

/**
 * Represents metadata configuration for a system, including website URL and terms of service (TOS) information.
 */
public class MetadataConfig implements Serializable {
    private String website;
    private String tos;

    /**
     * Get the URL of the system's website.
     *
     * @return The website URL.
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Set the URL of the system's website.
     *
     * @param website The website URL to set.
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * Get the terms of service (TOS) information for the system.
     *
     * @return The TOS information.
     */
    public String getTos() {
        return tos;
    }

    /**
     * Set the terms of service (TOS) information for the system.
     *
     * @param tos The TOS information to set.
     */
    public void setTos(String tos) {
        this.tos = tos;
    }
}
