package de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects;

import java.util.List;


/**
 * Represents the response structure for server information.
 * This class encapsulates details about the server, including metadata information and a list of provisioners.
 */
public class ServerInfoResponse {

    /**
     * The metadata information about the server.
     * This variable holds metadata details such as configuration settings, operational parameters,
     * and other relevant information about the server.
     * It is encapsulated in a {@link MetadataInfoResponse} object.
     */
    private MetadataInfoResponse metadataInfo;

    /**
     * The list of provisioners associated with the server.
     * This variable contains a collection of {@link ProvisionerResponse} objects,
     * each representing a provisioner responsible for handling specific aspects
     * of certificate management and related operations.
     */
    private List<ProvisionerResponse> provisioners;

    /**
     * Retrieves the metadata information of the server.
     * This metadata includes various configuration details and settings related to the server.
     *
     * @return The {@link MetadataInfoResponse} object containing server metadata.
     */
    public MetadataInfoResponse getMetadataInfo() {
        return metadataInfo;
    }

    /**
     * Sets the metadata information for the server.
     * This method allows updating the server's metadata information.
     *
     * @param metadataInfo The {@link MetadataInfoResponse} object to set as the server's metadata.
     */
    public void setMetadataInfo(MetadataInfoResponse metadataInfo) {
        this.metadataInfo = metadataInfo;
    }

    /**
     * Retrieves the list of provisioners associated with the server.
     * Each provisioner is responsible for handling specific aspects of certificate management.
     *
     * @return A list of {@link ProvisionerResponse} objects representing the server's provisioners.
     */
    public List<ProvisionerResponse> getProvisioners() {
        return provisioners;
    }

    /**
     * Sets the list of provisioners for the server.
     * This method allows updating the provisioners that are associated with the server.
     *
     * @param provisioners A list of {@link ProvisionerResponse} objects to set as the server's provisioners.
     */
    public void setProvisioners(List<ProvisionerResponse> provisioners) {
        this.provisioners = provisioners;
    }
}
