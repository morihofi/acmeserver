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
import de.morihofi.acmeserver.webui.handler.LoginUiHandler;
import de.morihofi.acmeserver.webui.handler.ProvisionerInfoHandler;
import de.morihofi.acmeserver.webui.handler.StatsHandler;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static de.morihofi.acmeserver.Main.appConfig;

public class WebUI {

    public static final String ATTR_LOCALE = "locale";

    public enum FRONTEND_PAGES {
        INDEX("/", "web.core.menu.home", "fa-solid fa-house-chimney"),
        STATISTICS("/stats", "web.core.menu.stats", "fa-solid fa-chart-simple"),
        COMMAND_BUILDER("/cmd-builder", "web.core.menu.commandBuilder", "fa-solid fa-terminal");

        private String route;
        private String translationKey;
        private String iconClass;

        FRONTEND_PAGES(String route, String translationKey, String iconClass) {
            this.route = route;
            this.translationKey = translationKey;
            this.iconClass = iconClass;
        }

        public String getRoute() {
            return route;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public String getIconClass() {
            return iconClass;
        }
    }

    public enum FRONTEND_ADMIN_PAGES {
        DASHBOARD("/mgmt", "web.admin.menu.dashboard", "me-2 fa-solid fa-gauge-high", false),
        SECURITY("/mgmt/security", "web.admin.menu.security", "me-2 fa-solid fa-shield-halved", true),
        ISSUED_CERTIFICATES("/mgmt/issues-certificates", "web.admin.menu.issuedCertificates", "me-2 fa-solid fa-certificate", false),
        LOGS("/mgmt/logs", "web.admin.menu.logs", "me-2 fa-solid fa-book", true),
        CONFIGURATION("/mgmt/configuration", "web.admin.menu.configuration", "me-2 fa-solid fa-wrench", true);

        private String route;
        private String translationKey;
        private String iconClass;
        private boolean onlyAdmin;

        FRONTEND_ADMIN_PAGES(String route, String translationKey, String iconClass, boolean onlyAdmin) {
            this.route = route;
            this.translationKey = translationKey;
            this.iconClass = iconClass;
            this.onlyAdmin = onlyAdmin;
        }

        public String getRoute() {
            return route;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public String getIconClass() {
            return iconClass;
        }
    }

    /**
     * Logger
     */
    public static final Logger log = LogManager.getLogger(WebUI.class);

    public static Map<String, Object> getDefaultFrontendMap(CryptoStoreManager cryptoStoreManager, Context context){
        return Map.of(
                "serverInfoResponse", ServerInfoEndpoint.getServerInfoResponse(appConfig.getProvisioner()),
                "cryptoStoreManager", cryptoStoreManager,
                "localizer", JteLocalizer.getLocalizerFromContext(context),
                "context", context
        );
    }

    public static void init(Javalin app, CryptoStoreManager cryptoStoreManager) {
        log.info("Initializing WebUI and registering routes ...");

        // Default routes
        app.get(FRONTEND_PAGES.INDEX.getRoute(), context -> context.render("pages/index.jte", getDefaultFrontendMap(cryptoStoreManager, context)));
        app.get(FRONTEND_PAGES.STATISTICS.getRoute(), new StatsHandler(cryptoStoreManager));
        app.get("/provisioner-info", new ProvisionerInfoHandler(cryptoStoreManager));
        app.get(FRONTEND_PAGES.COMMAND_BUILDER.getRoute(), context -> context.render("pages/cmd-builder.jte", getDefaultFrontendMap(cryptoStoreManager, context)));

        //Login
        app.get("/login", new LoginUiHandler(cryptoStoreManager));
        app.post("/login", new LoginUiHandler(cryptoStoreManager));


        // Admin routes
        app.get(FRONTEND_ADMIN_PAGES.DASHBOARD.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap(cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.SECURITY.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap(cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.ISSUED_CERTIFICATES.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap(cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.LOGS.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap(cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.CONFIGURATION.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap(cryptoStoreManager, context)));



    }
}
