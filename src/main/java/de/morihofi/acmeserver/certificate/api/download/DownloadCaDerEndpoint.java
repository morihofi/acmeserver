package de.morihofi.acmeserver.certificate.api.download;

import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class DownloadCaDerEndpoint implements Handler {
    private final CryptoStoreManager cryptoStoreManager;
    public DownloadCaDerEndpoint(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        byte[] der = cryptoStoreManager.getKeyStore()
                .getCertificate(CryptoStoreManager.KEYSTORE_ALIAS_ROOTCA)
                .getEncoded();

        ctx.result(der);
    }
}
