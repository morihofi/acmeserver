package de.morihofi.acmeserver.api.provisioner.byname.responses;

import com.google.gson.annotations.SerializedName;

public class ProvisionerByNameInfoResponse {
    @SerializedName("terms-of-service")
    private String termsOfService;
    @SerializedName("website")
    private String website;
    @SerializedName("allow-ip")
    private boolean ipAllowed;
    @SerializedName("allow-dns-wildcards")
    private boolean dnsWildcardAllowed;
    @SerializedName("crl-url")
    private String crlUrl;
    @SerializedName("ocsp-url")
    private String ocspUrl;


    public String getCrlUrl() {
        return crlUrl;
    }

    public void setCrlUrl(String crlUrl) {
        this.crlUrl = crlUrl;
    }

    public String getOcspUrl() {
        return ocspUrl;
    }

    public void setOcspUrl(String ocspUrl) {
        this.ocspUrl = ocspUrl;
    }

    public boolean isDnsWildcardAllowed() {
        return dnsWildcardAllowed;
    }

    public void setDnsWildcardAllowed(boolean dnsWildcardAllowed) {
        this.dnsWildcardAllowed = dnsWildcardAllowed;
    }

    public boolean isIpAllowed() {
        return ipAllowed;
    }

    public void setIpAllowed(boolean ipAllowed) {
        this.ipAllowed = ipAllowed;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }
}
