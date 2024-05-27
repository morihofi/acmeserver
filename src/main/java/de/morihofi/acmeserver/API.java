/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver;

import de.morihofi.acmeserver.api.provisioner.statistics.ProvisionerGlobalStatisticHandler;
import de.morihofi.acmeserver.api.provisioner.ProvisionerListHandler;
import de.morihofi.acmeserver.api.provisioner.statistics.ProvisionerStatisticHandler;
import de.morihofi.acmeserver.api.download.DownloadCaCabHandler;
import de.morihofi.acmeserver.api.download.DownloadCaDerHandler;
import de.morihofi.acmeserver.api.download.DownloadCaPemHandler;
import de.morihofi.acmeserver.api.serverInfo.ApiServerInfoEndpoint;
import de.morihofi.acmeserver.api.provisioner.statistics.ApiStatsProvisionerCertificatesIssued;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.Javalin;

public class API {

    public static void init(Javalin app, Config appConfig, CryptoStoreManager cryptoStoreManager) {
        // CA Downloads
        app.get("/ca.crt", new DownloadCaPemHandler(cryptoStoreManager));
        app.get("/ca.pem", new DownloadCaPemHandler(cryptoStoreManager));
        app.get("/ca.der", new DownloadCaDerHandler(cryptoStoreManager));
        app.get("/ca.cab", new DownloadCaCabHandler(cryptoStoreManager));

        // API Endpoints
        app.get("/api/serverinfo", new ApiServerInfoEndpoint(appConfig.getProvisioner()));
        app.get("/api/stats/provisioners/certificates-issued", new ApiStatsProvisionerCertificatesIssued());
        app.get("/api/provisioner/list", new ProvisionerListHandler(cryptoStoreManager));
        app.get("/api/provisioner/stats", new ProvisionerStatisticHandler(cryptoStoreManager));
        app.get("/api/provisioner/stats-global", new ProvisionerGlobalStatisticHandler(cryptoStoreManager));

    }
}
