package de.morihofi.acmeserver.config;

import java.io.Serializable;

public class CertificateMetadata implements Serializable {
    private String commonName;

    public String getCommonName() {
        return this.commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
}
