package de.morihofi.acmeserver;

import de.morihofi.acmeserver.certificate.api.download.DownloadCaCabEndpoint;
import de.morihofi.acmeserver.certificate.api.download.DownloadCaDerEndpoint;
import de.morihofi.acmeserver.certificate.api.download.DownloadCaPemEndpoint;
import de.morihofi.acmeserver.certificate.api.serverInfo.ApiServerInfoEndpoint;
import de.morihofi.acmeserver.certificate.api.statistics.ApiStatsProvisionerCertificatesIssued;
import de.morihofi.acmeserver.config.Config;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.Javalin;

public class API {

    public static void init(Javalin app, Config appConfig, CryptoStoreManager cryptoStoreManager) {
        app.get("/api/serverinfo", new ApiServerInfoEndpoint(appConfig.getProvisioner()));
        app.get("/ca.crt", new DownloadCaPemEndpoint(cryptoStoreManager));
        app.get("/ca.pem", new DownloadCaPemEndpoint(cryptoStoreManager));
        app.get("/ca.der", new DownloadCaDerEndpoint(cryptoStoreManager));
        app.get("/ca.cab", new DownloadCaCabEndpoint(cryptoStoreManager));

        app.get("/api/stats/provisioners/certificates-issued", new ApiStatsProvisionerCertificatesIssued());
    }
}
