/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.webui;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.ServerInfoEndpoint;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import io.javalin.Javalin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static de.morihofi.acmeserver.Main.appConfig;

public class WebUI {

    public static final String ATTR_LOCALE = "locale";

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(WebUI.class);

    public static void init(Javalin app, CryptoStoreManager cryptoStoreManager) {
        log.info("Initializing WebUI and registering routes ...");
        app.get("/", context -> {



            context.render("pages/index.jte",
                    Map.of(
                            "serverInfoResponse", ServerInfoEndpoint.getServerInfoResponse(appConfig.getProvisioner()),
                            "cryptoStoreManager", cryptoStoreManager,
                            "localizer", JteLocalizer.getLocalizerFromContext(context)
                    ));
        });

    }
}
