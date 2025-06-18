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
package de.morihofi.acmeserver.core.api.mgmt.provisioner.statistics;

import de.morihofi.acmeserver.core.api.mgmt.provisioner.statistics.responses.ProvisionerStatisticResponse;
import de.morihofi.acmeserver.core.api.mgmt.provisioner.ProvisionerStatistics;
import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;
import de.morihofi.acmeserver.types.intf.IServerInstance;

import lombok.NonNull;
import org.hibernate.Session;


/**
 * Handler for retrieving global statistics for all available provisioners.
 *
 * <p>This handler processes requests to retrieve global statistics of all provisioners,
 * including the total number of ACME accounts, issued certificates, revoked certificates,
 * and certificates waiting for issuance.</p>
 */
public class ProvisionerGlobalStatisticHandler implements Handler {

    /**
     * The IServerInstance that holds server configuration and utilities.
     */
    private final IServerInstance serverInstance;

    /**
     * Constructs a new ProvisionerGlobalStatisticHandler with the specified IServerInstance.
     *
     * @param serverInstance The IServerInstance used for accessing configuration and utilities.
     */
    public ProvisionerGlobalStatisticHandler(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles the request to retrieve global statistics for all provisioners.
     *
     * @param context The Javalin context for the current request.
     * @throws Exception If an error occurs while processing the request.
     */
    @Override
    public void handle(@NonNull HandlerContext context) throws Exception {
        ProvisionerStatisticResponse globalStats = new ProvisionerStatisticResponse();
        globalStats.setName(null);

        try (Session session = serverInstance.getDatabaseSession()) {
            globalStats.setAcmeAccounts(ProvisionerStatistics.countGlobalActiveACMEAccounts(session));
            globalStats.setCertificatesIssued(ProvisionerStatistics.countGlobalIssuedCertificates(session));
            globalStats.setCertificatesRevoked(ProvisionerStatistics.countGlobalRevokedCertificates(session));
            globalStats.setCertificatesIssueWaiting(ProvisionerStatistics.countGlobalCertificatesWaiting(session));
        }

        context.json(globalStats);
    }
}
