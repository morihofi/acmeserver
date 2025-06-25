/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.core.api.mgmt.provisioner;

import de.morihofi.acmeserver.core.api.mgmt.provisioner.statistics.responses.ProvisionerListEntryResponse;

import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.acmeserver.types.intf.IServerInstance;

import lombok.NonNull;


import java.util.ArrayList;
import java.util.List;

/**
 * Handler to list all available provisioners.
 *
 * <p>This handler is responsible for processing requests to list all available provisioners.
 * It generates a list of {@link ProvisionerListEntryResponse} objects containing information
 * about each provisioner and returns it as a JSON response.</p>
 */
public class ProvisionerListHandler implements Handler {

    /**
     * The server instance containing configuration and utility objects.
     */
    private final IServerInstance serverInstance;

    /**
     * Constructs a new {@link ProvisionerListHandler} with the specified server instance.
     *
     * @param serverInstance The server instance containing configuration and utility objects.
     */
    public ProvisionerListHandler(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles the request to list all available provisioners.
     *
     * @param context The Javalin context for the request.
     * @throws Exception If an error occurs while processing the request.
     */
    @Override
    public void handle(@NonNull HandlerContext context) throws Exception {

        List<ProvisionerListEntryResponse> provisionerResponse = new ArrayList<>();

        for (AcmeProvisioner provisioner : AcmeProvisioner.getAllProvisioners(serverInstance)) {
            ProvisionerListEntryResponse provisionerEntry = new ProvisionerListEntryResponse();
            provisionerEntry.setName(provisioner.getName());
            provisionerEntry.setDirectoryUrl(provisioner.getAcmeApiURL(serverInstance) + "/directory");

            provisionerResponse.add(provisionerEntry);
        }

        context.json(provisionerResponse);
    }
}
