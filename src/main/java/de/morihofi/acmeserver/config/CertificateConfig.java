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

import de.morihofi.acmeserver.config.certificateAlgorithms.AlgorithmParams;
import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents a configuration for certificates.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class CertificateConfig implements Serializable {
    @ConfigurationField(name = "Certificate Metadata")
    private CertificateMetadata metadata;
    @ConfigurationField(name = "Algorithm")
    private AlgorithmParams algorithm;
    @ConfigurationField(name = "Certificate Expiration")
    private CertificateExpiration expiration;

    /**
     * Get the metadata associated with the certificate.
     *
     * @return The certificate metadata.
     */
    public CertificateMetadata getMetadata() {
        return this.metadata;
    }

    /**
     * Set the metadata for the certificate.
     *
     * @param metadata The certificate metadata to set.
     */

    public void setMetadata(CertificateMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Get the expiration information for the certificate.
     *
     * @return The certificate expiration information.
     */
    public CertificateExpiration getExpiration() {
        return this.expiration;
    }

    /**
     * Set the expiration information for the certificate.
     *
     * @param expiration The certificate expiration information to set.
     */
    public void setExpiration(CertificateExpiration expiration) {
        this.expiration = expiration;
    }

    /**
     * Get the algorithm parameters for the certificate.
     *
     * @return The certificate algorithm parameters.
     */
    public AlgorithmParams getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the algorithm parameters for the certificate.
     *
     * @param algorithm The certificate algorithm parameters to set.
     */
    public void setAlgorithm(AlgorithmParams algorithm) {
        this.algorithm = algorithm;
    }
}
