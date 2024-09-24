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
package de.morihofi.acmeserver.api.provisioner.statistics;

import de.morihofi.acmeserver.api.provisioner.statistics.responses.ProvisionerStatisticResponse;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerStatistics;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.tools.ServerInstance;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for retrieving global statistics for all available provisioners.
 *
 * <p>This handler processes requests to retrieve global statistics of all provisioners,
 * including the total number of ACME accounts, issued certificates, revoked certificates,
 * and certificates waiting for issuance.</p>
 */
public class ProvisionerGlobalStatisticHandler implements Handler {

    /**
     * The ServerInstance that holds server configuration and utilities.
     */
    private final ServerInstance serverInstance;

    /**
     * Constructs a new ProvisionerGlobalStatisticHandler with the specified ServerInstance.
     *
     * @param serverInstance The ServerInstance used for accessing configuration and utilities.
     */
    public ProvisionerGlobalStatisticHandler(ServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles the request to retrieve global statistics for all provisioners.
     *
     * @param context The Javalin context for the current request.
     * @throws Exception If an error occurs while processing the request.
     */
    @Override
    public void handle(@NotNull Context context) throws Exception {
        ProvisionerStatisticResponse globalStats = new ProvisionerStatisticResponse();
        globalStats.setName(null);

        try (Session session = serverInstance.getHibernateUtil().getSessionFactory().openSession()) {
            globalStats.setAcmeAccounts(ProvisionerStatistics.countGlobalActiveACMEAccounts(session));
            globalStats.setCertificatesIssued(ProvisionerStatistics.countGlobalIssuedCertificates(session));
            globalStats.setCertificatesRevoked(ProvisionerStatistics.countGlobalRevokedCertificates(session));
            globalStats.setCertificatesIssueWaiting(ProvisionerStatistics.countGlobalCertificatesWaiting(session));
        }

        context.json(globalStats);
    }
}
