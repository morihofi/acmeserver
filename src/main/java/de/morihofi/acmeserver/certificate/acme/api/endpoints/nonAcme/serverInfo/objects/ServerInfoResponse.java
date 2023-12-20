package de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects;

import java.util.List;

public class ServerInfoResponse {
    private MetadataInfoResponse metadataInfo;
    private List<ProvisionerResponse> provisioners;

    public MetadataInfoResponse getMetadataInfo() {
        return metadataInfo;
    }

    public void setMetadataInfo(MetadataInfoResponse metadataInfo) {
        this.metadataInfo = metadataInfo;
    }

    public List<ProvisionerResponse> getProvisioners() {
        return provisioners;
    }

    public void setProvisioners(List<ProvisionerResponse> provisioners) {
        this.provisioners = provisioners;
    }
}
