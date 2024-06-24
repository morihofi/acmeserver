/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.api.provisioner.statistics;

import de.morihofi.acmeserver.api.provisioner.statistics.responses.ProvisionerStatisticResponse;
import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.certificate.provisioners.ProvisionerStatistics;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.tools.ServerInstance;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for retrieving statistics of all available provisioners.
 *
 * <p>This class implements the {@link Handler} interface to handle HTTP GET requests for fetching provisioner statistics. It utilizes
 * Hibernate for database interactions and gathers various statistics like the number of ACME accounts, issued certificates, revoked
 * certificates, and certificates waiting for issuance for each provisioner.
 */
public class ProvisionerStatisticHandler implements Handler {

    /**
     * The server instance containing necessary configurations and utilities.
     */
    private final ServerInstance serverInstance;

    /**
     * Constructs a new {@link ProvisionerStatisticHandler} with the specified {@link ServerInstance}.
     *
     * @param serverInstance The server instance containing necessary configurations and utilities.
     */
    public ProvisionerStatisticHandler(ServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Handles the HTTP request to retrieve statistics of all available provisioners.
     *
     * <p>This method is annotated with {@link OpenApi} to define its OpenAPI documentation. It gathers statistics for each provisioner and
     * returns them in a JSON format.
     *
     * @param context The Javalin {@link Context} of the HTTP request.
     * @throws Exception If an error occurs while retrieving the statistics.
     */
    @Override
    @OpenApi(
            summary = "List statistics of all available provisioner",
            operationId = "getProvisionerStats",
            path = "/api/stats/provisioner-all",
            methods = HttpMethod.GET,
            tags = {"Statistics"},
            responses = {
                    @OpenApiResponse(status = "200", content = {
                            @OpenApiContent(
                                    from = ProvisionerStatisticResponse[].class,
                                    mimeType = "application/json"
                            )
                    })
            }
    )
    public void handle(@NotNull Context context) throws Exception {
        List<ProvisionerStatisticResponse> statisticItemsOfProvisioner = new ArrayList<>();

        try (Session session = serverInstance.getHibernateUtil().getSessionFactory().openSession()) {
            for (Provisioner provisioner : ProvisionerManager.getProvisioners()) {
                String provisionerName = provisioner.getProvisionerName();

                ProvisionerStatisticResponse item = new ProvisionerStatisticResponse();
                item.setName(provisionerName);
                item.setAcmeAccounts(ProvisionerStatistics.countACMEAccountsByProvisioner(session, provisionerName));
                item.setCertificatesIssued(ProvisionerStatistics.countIssuedCertificatesByProvisioner(session, provisionerName));
                item.setCertificatesRevoked(ProvisionerStatistics.countRevokedCertificatesByProvisioner(session, provisionerName));
                item.setCertificatesIssueWaiting(
                        ProvisionerStatistics.countCertificatesWaitingForIssueByProvisioner(session, provisionerName));

                statisticItemsOfProvisioner.add(item);
            }
        }

        context.json(statisticItemsOfProvisioner);
    }
}
