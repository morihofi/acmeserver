package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.config.certificateAlgorithms.AlgorithmParams;

import java.io.Serializable;

public class CertificateConfig implements Serializable {
    private CertificateMetadata metadata;
    private AlgorithmParams algorithm;
    private CertificateExpiration expiration;

    public CertificateMetadata getMetadata() {
        return this.metadata;
    }

    public void setMetadata(CertificateMetadata metadata) {
        this.metadata = metadata;
    }


    public CertificateExpiration getExpiration() {
        return this.expiration;
    }

    public void setExpiration(CertificateExpiration expiration) {
        this.expiration = expiration;
    }

    public AlgorithmParams getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmParams algorithm) {
        this.algorithm = algorithm;
    }
}