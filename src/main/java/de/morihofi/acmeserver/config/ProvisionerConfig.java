/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents configuration for a provisioner, including its name, intermediate certificate, metadata, issued certificate expiration, domain
 * name restrictions, and wildcard allowance.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ProvisionerConfig implements Serializable {
    @ConfigurationField(name = "Provisioner name", required = true)
    private String name;
    @ConfigurationField(name = "Intermediate Certificate Settings")
    private CertificateConfig intermediate;
    @ConfigurationField(name = "Provisioner metadata")
    private MetadataConfig meta;
    @ConfigurationField(name = "Issued certificate expiration")
    private CertificateExpiration issuedCertificateExpiration;
    @ConfigurationField(name = "Domain Name Restriction")
    private DomainNameRestrictionConfig domainNameRestriction;
    @ConfigurationField(name = "Allow issuing for DNS Wildcards")
    private boolean wildcardAllowed;
    @ConfigurationField(name = "Allow issuing for IP Addresses")
    private boolean ipAllowed;

    /**
     * Get the name of the provisioner.
     *
     * @return The provisioner name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of the provisioner.
     *
     * @param name The provisioner name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the intermediate certificate for the provisioner.
     *
     * @return The intermediate certificate.
     */
    public CertificateConfig getIntermediate() {
        return this.intermediate;
    }

    /**
     * Set the intermediate certificate for the provisioner.
     *
     * @param intermediate The intermediate certificate to set.
     */
    public void setIntermediate(CertificateConfig intermediate) {
        this.intermediate = intermediate;
    }

    /**
     * Get the metadata configuration for the provisioner.
     *
     * @return The metadata configuration.
     */
    public MetadataConfig getMeta() {
        return meta;
    }

    /**
     * Set the metadata configuration for the provisioner.
     *
     * @param meta The metadata configuration to set.
     */
    public void setMeta(MetadataConfig meta) {
        this.meta = meta;
    }

    /**
     * Get the expiration information for issued certificates by the provisioner.
     *
     * @return The expiration information for issued certificates.
     */
    public CertificateExpiration getIssuedCertificateExpiration() {
        return issuedCertificateExpiration;
    }

    /**
     * Set the expiration information for issued certificates by the provisioner.
     *
     * @param issuedCertificateExpiration The expiration information to set.
     */
    public void setIssuedCertificateExpiration(CertificateExpiration issuedCertificateExpiration) {
        this.issuedCertificateExpiration = issuedCertificateExpiration;
    }

    /**
     * Get the domain name restriction configuration for the provisioner.
     *
     * @return The domain name restriction configuration.
     */
    public DomainNameRestrictionConfig getDomainNameRestriction() {
        return domainNameRestriction;
    }

    /**
     * Set the domain name restriction configuration for the provisioner.
     *
     * @param domainNameRestriction The domain name restriction configuration to set.
     */
    public void setDomainNameRestriction(DomainNameRestrictionConfig domainNameRestriction) {
        this.domainNameRestriction = domainNameRestriction;
    }

    /**
     * Check if wildcard certificates are allowed for this provisioner.
     *
     * @return True if wildcard certificates are allowed, false otherwise.
     */
    public boolean isWildcardAllowed() {
        return wildcardAllowed;
    }

    /**
     * Set whether wildcard certificates are allowed for this provisioner.
     *
     * @param wildcardAllowed True if wildcard certificates are allowed, false otherwise.
     */
    public void setWildcardAllowed(boolean wildcardAllowed) {
        this.wildcardAllowed = wildcardAllowed;
    }

    public boolean isIpAllowed() {
        return ipAllowed;
    }

    public void setIpAllowed(boolean ipAllowed) {
        this.ipAllowed = ipAllowed;
    }
}
