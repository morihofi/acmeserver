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

package de.morihofi.acmeserver.webui;

import de.morihofi.acmeserver.Main;
import de.morihofi.acmeserver.certificate.api.serverInfo.ApiServerInfoEndpoint;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.webui.handler.CommandBuilderHandler;
import de.morihofi.acmeserver.webui.handler.IndexHandler;
import de.morihofi.acmeserver.webui.handler.ProvisionerInfoHandler;
import de.morihofi.acmeserver.webui.handler.StatsHandler;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static de.morihofi.acmeserver.Main.appConfig;

public class WebUI {

    public static final String ATTR_LOCALE = "locale";
    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().getClass());

    /**
     * Constructs a default map of objects to be used by the frontend, encapsulating various pieces of information and utility objects
     * necessary for frontend operations. This map includes server information, cryptographic store management details, localization
     * utilities, and the current request context.
     *
     * <p>The map contains the following keys and their associated values:</p>
     * <ul>
     *     <li>{@code serverInfoResponse} - Contains response data from the server information endpoint, typically
     *     including server status, version, and other relevant details.</li>
     *     <li>{@code cryptoStoreManager} - The {@link CryptoStoreManager} instance being used by the application
     *     for cryptographic operations and key management.</li>
     *     <li>{@code localizer} - A localizer utility derived from the current context, used for internationalization
     *     and localization of the frontend content.</li>
     *     <li>{@code context} - The current {@link Context} instance, representing the request and response context
     *     within the application.</li>
     * </ul>
     *
     * @param cryptoStoreManager the cryptographic store manager used by the application.
     * @param context            the current request context.
     * @return a {@link Map} containing default objects and information for frontend use.
     */
    public static Map<String, Object> getDefaultFrontendMap(CryptoStoreManager cryptoStoreManager, Context context) {
        return Map.of(
                "serverInfoResponse", ApiServerInfoEndpoint.getServerInfoResponse(appConfig.getProvisioner()),
                "cryptoStoreManager", cryptoStoreManager,
                "localizer", JteLocalizer.getLocalizerFromContext(context),
                "context", context
        );
    }

    public static void init(Javalin app, CryptoStoreManager cryptoStoreManager) {
        LOG.info("Initializing WebUI and registering routes ...");

        // Default routes
        app.get(FRONTEND_PAGES.INDEX.getRoute(), new IndexHandler(cryptoStoreManager));
        app.get(FRONTEND_PAGES.STATISTICS.getRoute(), new StatsHandler(cryptoStoreManager));
        app.get("/provisioner-info", new ProvisionerInfoHandler(cryptoStoreManager));
        app.get(FRONTEND_PAGES.COMMAND_BUILDER.getRoute(), new CommandBuilderHandler(cryptoStoreManager));

        // Login
/*
        app.get("/login", new LoginUiHandler(cryptoStoreManager));
        app.post("/login", new LoginUiHandler(cryptoStoreManager));
*/

        // Admin routes
/*
        app.get(FRONTEND_ADMIN_PAGES.DASHBOARD.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap
        (cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.SECURITY.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap
        (cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.ISSUED_CERTIFICATES.getRoute(), context -> context.render("pages/mgmt/index.jte",
        getDefaultFrontendMap(cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.LOGS.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap
        (cryptoStoreManager, context)));
        app.get(FRONTEND_ADMIN_PAGES.CONFIGURATION.getRoute(), context -> context.render("pages/mgmt/index.jte", getDefaultFrontendMap
        (cryptoStoreManager, context)));
*/
    }

    public static TemplateEngine createTemplateEngine() {
        boolean isDev = !isRunningFromJar();

        if (isDev) {
            LOG.info("Looks like this application is running from an IDE or outside a jar, using a JRE compiler resolver");

            DirectoryCodeResolver codeResolver = new DirectoryCodeResolver(Path.of("src", "main", "jte"));
            return TemplateEngine.create(codeResolver, ContentType.Html);
        } else {
            LOG.info("Running inside a JAR -> using precompiled classes for web ui");

            return TemplateEngine.createPrecompiled(ContentType.Html);
        }
    }

    public static boolean isRunningFromJar() {
        // The path to a class that exists safely in the JAR.
        // We're using our main(args[]) class here
        String className = Main.class.getName().replace('.', '/') + ".class";
        URL classURL = Main.class.getClassLoader().getResource(className);
        return classURL != null && classURL.getProtocol().equals("jar");
    }

    public static boolean isLegacyBrowser(Context context) {
        String userAgent = context.userAgent();
        // System.out.println(userAgent);

        try {
            // Überprüfen, ob der User-Agent-String auf IE 9 oder älter hinweist
            assert userAgent != null;
            if (userAgent.contains("MSIE")) {
                // Extrahiere die Version von IE aus dem User-Agent-String
                String versionString = userAgent.substring(userAgent.indexOf("MSIE") + 5);
                versionString = versionString.substring(0, versionString.indexOf(";"));
                double version = Double.parseDouble(versionString);

                // Überprüfe, ob die Version ≤ 9 ist
                if (version <= 9.0) {
                    return true;
                }
            }

            // Überprüfe auf Pocket Internet Explorer durch spezifische Schlüsselwörter im User-Agent-String
            // Beachte, dass es viele Variationen des User-Agent-Strings für Pocket IE gibt
            // und diese Überprüfung möglicherweise angepasst werden muss, um spezifische Versionen zu erfassen
            if (userAgent.contains("Windows CE") || userAgent.contains("IEMobile")) {
                return true;
            }
        } catch (Exception ex) {
            LOG.error("Error checking if User Agent {} is a legacy browser", userAgent, ex);
        }

        return false;
    }

    public enum FRONTEND_PAGES {
        INDEX("/", "web.core.menu.home", "fa-solid fa-house-chimney"),
        STATISTICS("/stats", "web.core.menu.stats", "fa-solid fa-chart-simple"),
        COMMAND_BUILDER("/cmd-builder", "web.core.menu.commandBuilder", "fa-solid fa-terminal");

        private final String route;
        private final String translationKey;
        private final String iconClass;

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

        private final String route;
        private final String translationKey;
        private final String iconClass;
        private final boolean onlyAdmin;

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
}
