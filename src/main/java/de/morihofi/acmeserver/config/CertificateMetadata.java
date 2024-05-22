package de.morihofi.acmeserver.config;

import java.io.Serializable;

/**
 * Represents metadata for a certificate, including common name, organization, organizational unit, and country code.
 */
public class CertificateMetadata implements Serializable {
    private String commonName;
    private String organisation;
    private String organisationalUnit;
    private String countryCode;

    /**
     * Get the common name for the certificate metadata.
     *
     * @return The common name.
     */
    public String getCommonName() {
        return this.commonName;
    }

    /**
     * Set the common name for the certificate metadata.
     *
     * @param commonName The common name to set.
     */
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    /**
     * Get the organization for the certificate metadata.
     *
     * @return The organization.
     */
    public String getOrganisation() {
        return organisation;
    }

    /**
     * Set the organization for the certificate metadata.
     *
     * @param organisation The organization to set.
     */
    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    /**
     * Get the organizational unit for the certificate metadata.
     *
     * @return The organizational unit.
     */
    public String getOrganisationalUnit() {
        return organisationalUnit;
    }

    /**
     * Set the organizational unit for the certificate metadata.
     *
     * @param organisationalUnit The organizational unit to set.
     */
    public void setOrganisationalUnit(String organisationalUnit) {
        this.organisationalUnit = organisationalUnit;
    }

    /**
     * Get the country code for the certificate metadata.
     *
     * @return The country code.
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Set the country code for the certificate metadata.
     *
     * @param countryCode The country code to set.
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
