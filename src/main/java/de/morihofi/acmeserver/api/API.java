/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.api;

import de.morihofi.acmeserver.api.download.DownloadCaCabHandler;
import de.morihofi.acmeserver.api.download.DownloadCaDerHandler;
import de.morihofi.acmeserver.api.download.DownloadCaPemHandler;
import de.morihofi.acmeserver.api.provisioner.ProvisionerListHandler;
import de.morihofi.acmeserver.api.provisioner.byname.ProvisionerByNameInfoHandler;
import de.morihofi.acmeserver.api.provisioner.statistics.ApiStatsProvisionerCertificatesIssued;
import de.morihofi.acmeserver.api.provisioner.statistics.ProvisionerGlobalStatisticHandler;
import de.morihofi.acmeserver.api.provisioner.statistics.ProvisionerStatisticHandler;
import de.morihofi.acmeserver.api.serverInfo.ApiServerInfoEndpoint;
import de.morihofi.acmeserver.api.troubleshooting.DnsResolverHandler;
import de.morihofi.acmeserver.tools.ServerInstance;
import io.javalin.Javalin;

/**
 * This class initializes and configures the API endpoints for the ACME server application.
 */
public class API {

    /**
     * Initializes the API endpoints on the given Javalin app instance using the provided server instance.
     *
     * @param app           The Javalin app instance to configure.
     * @param serverInstance The server instance providing necessary server configurations and services.
     */
    public static void init(Javalin app, ServerInstance serverInstance) {
        // CA Downloads
        app.get("/ca.crt", new DownloadCaPemHandler(serverInstance));
        app.get("/ca.pem", new DownloadCaPemHandler(serverInstance));
        app.get("/ca.der", new DownloadCaDerHandler(serverInstance));
        app.get("/ca.cab", new DownloadCaCabHandler(serverInstance));

        // API Endpoints
        app.get("/api/serverinfo", new ApiServerInfoEndpoint(serverInstance));
        app.get("/api/stats/provisioners/certificates-issued", new ApiStatsProvisionerCertificatesIssued(serverInstance));

        app.get("/api/provisioner/list", new ProvisionerListHandler(serverInstance));
        app.get("/api/provisioner/by-name/{provisionerName}/info", new ProvisionerByNameInfoHandler(serverInstance));
        // Statistics
        app.get("/api/stats/provisioner/all", new ProvisionerStatisticHandler(serverInstance));
        app.get("/api/stats/provisioner/global", new ProvisionerGlobalStatisticHandler(serverInstance));
        // Troubleshooting
        app.post("/api/troubleshooting/dns-resolver", new DnsResolverHandler(serverInstance));

        // TODO: Implement config production ready
        // Config
        // app.get("/api/config/layout", new ConfigLayoutHandler(cryptoStoreManager));
        // app.get("/api/config/current", new ConfigHandler(cryptoStoreManager));
    }
}
