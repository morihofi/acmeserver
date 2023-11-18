package de.morihofi.acmeserver.config;

import java.io.Serializable;

public class ProvisionerConfig implements Serializable {
    private String name;
    private boolean useThisProvisionerIntermediateForAcmeApi;

    private CertificateConfig intermediate;

    private MetadataConfig meta;

    public MetadataConfig getMeta() {
        return meta;
    }

    public void setMeta(MetadataConfig meta) {
        this.meta = meta;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CertificateConfig getIntermediate() {
        return this.intermediate;
    }

    public void setIntermediate(CertificateConfig intermediate) {
        this.intermediate = intermediate;
    }

    public boolean isUseThisProvisionerIntermediateForAcmeApi() {
        return useThisProvisionerIntermediateForAcmeApi;
    }

    public void setUseThisProvisionerIntermediateForAcmeApi(boolean useThisProvisionerIntermediateForAcmeApi) {
        this.useThisProvisionerIntermediateForAcmeApi = useThisProvisionerIntermediateForAcmeApi;
    }
}