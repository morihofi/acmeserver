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

package de.morihofi.acmeserver.core.config;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents configuration for a provisioner, including its name, intermediate certificate, metadata, issued certificate expiration, domain
 * name restrictions, and wildcard allowance.
 */
@Data
public class ProvisionerConfig implements Serializable {
    /**
     * The name of the provisioner.
     */
    private String name;

    /**
     * The intermediate certificate settings for the provisioner.
     */
    private CertificateConfig intermediate = new CertificateConfig();

    /**
     * The metadata configuration for the provisioner.
     */
    private MetadataConfig meta = new MetadataConfig();

    /**
     * The expiration configuration for issued certificates by the provisioner.
     */
    private CertificateExpiration issuedCertificateExpiration;

    /**
     * The domain name restriction configuration for the provisioner.
     */
    private DomainNameRestrictionConfig domainNameRestriction = new DomainNameRestrictionConfig();

    /**
     * Flag indicating whether wildcard certificates are allowed for this provisioner.
     */
    private boolean wildcardAllowed = false;

    /**
     * Flag indicating whether IP address certificates are allowed for this provisioner.
     */
    private boolean ipAllowed = false;
}
