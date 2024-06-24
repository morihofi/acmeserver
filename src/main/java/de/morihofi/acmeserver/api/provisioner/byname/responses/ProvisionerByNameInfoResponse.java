package de.morihofi.acmeserver.api.provisioner.byname.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the response information for a provisioner by name.
 */
public class ProvisionerByNameInfoResponse {
    /**
     * The terms of service URL for the provisioner.
     */
    @SerializedName("terms-of-service")
    private String termsOfService;

    /**
     * The website URL for the provisioner.
     */
    @SerializedName("website")
    private String website;

    /**
     * Indicates whether IP issuance is allowed.
     */
    @SerializedName("allow-ip")
    private boolean ipAllowed;

    /**
     * Indicates whether DNS wildcard issuance is allowed.
     */
    @SerializedName("allow-dns-wildcards")
    private boolean dnsWildcardAllowed;

    /**
     * The CRL (Certificate Revocation List) URL for the provisioner.
     */
    @SerializedName("crl-url")
    private String crlUrl;

    /**
     * The OCSP (Online Certificate Status Protocol) URL for the provisioner.
     */
    @SerializedName("ocsp-url")
    private String ocspUrl;

    /**
     * Get the CRL URL for the provisioner.
     *
     * @return The CRL URL.
     */
    public String getCrlUrl() {
        return crlUrl;
    }

    /**
     * Set the CRL URL for the provisioner.
     *
     * @param crlUrl The CRL URL to set.
     */
    public void setCrlUrl(String crlUrl) {
        this.crlUrl = crlUrl;
    }

    /**
     * Get the OCSP URL for the provisioner.
     *
     * @return The OCSP URL.
     */
    public String getOcspUrl() {
        return ocspUrl;
    }

    /**
     * Set the OCSP URL for the provisioner.
     *
     * @param ocspUrl The OCSP URL to set.
     */
    public void setOcspUrl(String ocspUrl) {
        this.ocspUrl = ocspUrl;
    }

    /**
     * Check if DNS wildcard issuance is allowed.
     *
     * @return True if DNS wildcard issuance is allowed, false otherwise.
     */
    public boolean isDnsWildcardAllowed() {
        return dnsWildcardAllowed;
    }

    /**
     * Set whether DNS wildcard issuance is allowed.
     *
     * @param dnsWildcardAllowed True if DNS wildcard issuance is allowed, false otherwise.
     */
    public void setDnsWildcardAllowed(boolean dnsWildcardAllowed) {
        this.dnsWildcardAllowed = dnsWildcardAllowed;
    }

    /**
     * Check if IP issuance is allowed.
     *
     * @return True if IP issuance is allowed, false otherwise.
     */
    public boolean isIpAllowed() {
        return ipAllowed;
    }

    /**
     * Set whether IP issuance is allowed.
     *
     * @param ipAllowed True if IP issuance is allowed, false otherwise.
     */
    public void setIpAllowed(boolean ipAllowed) {
        this.ipAllowed = ipAllowed;
    }

    /**
     * Get the website URL for the provisioner.
     *
     * @return The website URL.
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Set the website URL for the provisioner.
     *
     * @param website The website URL to set.
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * Get the terms of service URL for the provisioner.
     *
     * @return The terms of service URL.
     */
    public String getTermsOfService() {
        return termsOfService;
    }

    /**
     * Set the terms of service URL for the provisioner.
     *
     * @param termsOfService The terms of service URL to set.
     */
    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }
}
