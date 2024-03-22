package de.morihofi.acmeserver.certificate.api.download;

import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class DownloadCaEndpoint implements Handler {

    /**
     * Logger
     */
    public final Logger log = LogManager.getLogger(getClass());

    /**
     * Instance for accessing the current provisioner
     */
    private final CryptoStoreManager cryptoStoreManager;

    /**
     * Endpoint for downloading the root certificate
     *
     * @param cryptoStoreManager Instance of {@link CryptoStoreManager} for accessing KeyStores
     */
    public DownloadCaEndpoint(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    /**
     * Method for handling the request
     *
     * @param ctx Javalin Context
     * @throws Exception thrown when there was an error processing the request
     */
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        ctx.header("Content-Type", "application/x-x509-ca-cert");

        String pem = PemUtil.certificateToPEM(
                cryptoStoreManager.getKeyStore()
                        .getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)
                        .getEncoded()
        );

        ctx.result(pem);
    }
}
