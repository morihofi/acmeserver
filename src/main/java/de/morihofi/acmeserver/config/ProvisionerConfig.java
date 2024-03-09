package de.morihofi.acmeserver.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents configuration for a provisioner, including its name, intermediate certificate, metadata, issued certificate expiration,
 * domain name restrictions, and wildcard allowance.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ProvisionerConfig implements Serializable {
    private String name;
    private CertificateConfig intermediate;
    private MetadataConfig meta;
    private CertificateExpiration issuedCertificateExpiration;
    private DomainNameRestrictionConfig domainNameRestriction;
    private boolean wildcardAllowed;

    /**
     * Get the name of the provisioner.
     * @return The provisioner name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of the provisioner.
     * @param name The provisioner name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the intermediate certificate for the provisioner.
     * @return The intermediate certificate.
     */
    public CertificateConfig getIntermediate() {
        return this.intermediate;
    }

    /**
     * Set the intermediate certificate for the provisioner.
     * @param intermediate The intermediate certificate to set.
     */
    public void setIntermediate(CertificateConfig intermediate) {
        this.intermediate = intermediate;
    }

    /**
     * Get the metadata configuration for the provisioner.
     * @return The metadata configuration.
     */
    public MetadataConfig getMeta() {
        return meta;
    }

    /**
     * Set the metadata configuration for the provisioner.
     * @param meta The metadata configuration to set.
     */
    public void setMeta(MetadataConfig meta) {
        this.meta = meta;
    }

    /**
     * Get the expiration information for issued certificates by the provisioner.
     * @return The expiration information for issued certificates.
     */
    public CertificateExpiration getIssuedCertificateExpiration() {
        return issuedCertificateExpiration;
    }

    /**
     * Set the expiration information for issued certificates by the provisioner.
     * @param issuedCertificateExpiration The expiration information to set.
     */
    public void setIssuedCertificateExpiration(CertificateExpiration issuedCertificateExpiration) {
        this.issuedCertificateExpiration = issuedCertificateExpiration;
    }

    /**
     * Get the domain name restriction configuration for the provisioner.
     * @return The domain name restriction configuration.
     */
    public DomainNameRestrictionConfig getDomainNameRestriction() {
        return domainNameRestriction;
    }

    /**
     * Set the domain name restriction configuration for the provisioner.
     * @param domainNameRestriction The domain name restriction configuration to set.
     */
    public void setDomainNameRestriction(DomainNameRestrictionConfig domainNameRestriction) {
        this.domainNameRestriction = domainNameRestriction;
    }

    /**
     * Check if wildcard certificates are allowed for this provisioner.
     * @return True if wildcard certificates are allowed, false otherwise.
     */
    public boolean isWildcardAllowed() {
        return wildcardAllowed;
    }

    /**
     * Set whether wildcard certificates are allowed for this provisioner.
     * @param wildcardAllowed True if wildcard certificates are allowed, false otherwise.
     */
    public void setWildcardAllowed(boolean wildcardAllowed) {
        this.wildcardAllowed = wildcardAllowed;
    }
}
