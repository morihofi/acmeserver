/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Represents metadata for a certificate, including common name, organization, organizational unit, and country code.
 */
public class CertificateMetadata implements Serializable {
    /**
     * Common name
     */
    @ConfigurationField(name = "Common Name", required = true)
    private String commonName = "";
    /**
     * Organisation
     */
    @ConfigurationField(name = "Organisation")
    private String organisation;
    /**
     * Organisational Unit
     */
    @ConfigurationField(name = "Organisational Unit")
    private String organisationalUnit;
    /**
     * ISO Country Code
     */
    @ConfigurationField(name = "Country Code")
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
