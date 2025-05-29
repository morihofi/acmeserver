package de.morihofi.acmeserver.core.api.mgmt.provisioner.byname.responses;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Represents the response information for a provisioner by name.
 */
@Data
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


}
