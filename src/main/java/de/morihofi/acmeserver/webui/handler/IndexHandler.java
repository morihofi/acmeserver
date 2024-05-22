package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.webui.WebUI;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class IndexHandler implements Handler {
    private final CryptoStoreManager cryptoStoreManager;

    public IndexHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        Map<String, Object> params = new HashMap<>(WebUI.getDefaultFrontendMap(cryptoStoreManager, context));

        context.render((WebUI.isLegacyBrowser(context) ? "legacy" : "html5") + "/pages/index.jte", params);
    }
}
