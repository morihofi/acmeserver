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

package de.morihofi.acmeserver.core.api.serverInfo.objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Represents the response structure for server information. This class encapsulates details about the server, including metadata
 * information and a list of provisioners.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ServerInfoResponse {

    /**
     * The metadata information about the server. This variable holds metadata details such as configuration settings, operational
     * parameters, and other relevant information about the server. It is encapsulated in a {@link MetadataInfoResponse} object.
     */
    private MetadataInfoResponse metadataInfo;

    /**
     * The list of provisioners associated with the server. This variable contains a collection of {@link ProvisionerResponse} objects, each
     * representing a provisioner responsible for handling specific aspects of certificate management and related operations.
     */
    private List<ProvisionerResponse> provisioners;

    /**
     * Retrieves the metadata information of the server. This metadata includes various configuration details and settings related to the
     * server.
     *
     * @return The {@link MetadataInfoResponse} object containing server metadata.
     */
    public MetadataInfoResponse getMetadataInfo() {
        return metadataInfo;
    }

    /**
     * Sets the metadata information for the server. This method allows updating the server's metadata information.
     *
     * @param metadataInfo The {@link MetadataInfoResponse} object to set as the server's metadata.
     */
    public void setMetadataInfo(MetadataInfoResponse metadataInfo) {
        this.metadataInfo = metadataInfo;
    }

    /**
     * Retrieves the list of provisioners associated with the server. Each provisioner is responsible for handling specific aspects of
     * certificate management.
     *
     * @return A list of {@link ProvisionerResponse} objects representing the server's provisioners.
     */
    public List<ProvisionerResponse> getProvisioners() {
        return provisioners;
    }

    /**
     * Sets the list of provisioners for the server. This method allows updating the provisioners that are associated with the server.
     *
     * @param provisioners A list of {@link ProvisionerResponse} objects to set as the server's provisioners.
     */
    public void setProvisioners(List<ProvisionerResponse> provisioners) {
        this.provisioners = provisioners;
    }
}
