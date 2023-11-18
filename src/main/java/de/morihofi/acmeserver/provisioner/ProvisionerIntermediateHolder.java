package de.morihofi.acmeserver.provisioner;

import java.security.cert.X509Certificate;

public class ProvisionerIntermediateHolder {
    private String provisionerName;

    private X509Certificate intermediateCertificate;

    public ProvisionerIntermediateHolder(String provisionerName) {
        this.provisionerName = provisionerName;
    }

    public String getProvisionerName() {
        return provisionerName;
    }

    public X509Certificate getIntermediateCertificate() {
        return intermediateCertificate;
    }
}
