package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.config.certificateAlgorithms.AlgorithmParams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents a configuration for certificates.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class CertificateConfig implements Serializable {
    private CertificateMetadata metadata;
    private AlgorithmParams algorithm;
    private CertificateExpiration expiration;

    /**
     * Get the metadata associated with the certificate.
     * @return The certificate metadata.
     */
    public CertificateMetadata getMetadata() {
        return this.metadata;
    }

    /**
     * Set the metadata for the certificate.
     * @param metadata The certificate metadata to set.
     */

    public void setMetadata(CertificateMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Get the expiration information for the certificate.
     * @return The certificate expiration information.
     */
    public CertificateExpiration getExpiration() {
        return this.expiration;
    }

    /**
     * Set the expiration information for the certificate.
     * @param expiration The certificate expiration information to set.
     */
    public void setExpiration(CertificateExpiration expiration) {
        this.expiration = expiration;
    }

    /**
     * Get the algorithm parameters for the certificate.
     * @return The certificate algorithm parameters.
     */
    public AlgorithmParams getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the algorithm parameters for the certificate.
     * @param algorithm The certificate algorithm parameters to set.
     */
    public void setAlgorithm(AlgorithmParams algorithm) {
        this.algorithm = algorithm;
    }
}
