/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.certgine.core.web;

import de.morihofi.certgine.types.config.Config;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.network.ssl.mozillasslconfig.MozillaSslConfigHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.util.ssl.SslContextFactory;

@Slf4j
public class JettySslHelper {

    public static void applyMozillaTlsConfig(MozillaSslConfigHelper.BasicConfiguration cfg,
                                             SslContextFactory.Server factory,
                                             SecureRequestCustomizer customizer) {
        if (cfg == null) {
            return;
        }

        log.info("Configuring TLS using Mozilla configuration with support for at least {}", String.join(", ", cfg.oldestClients()));
        factory.setExcludeProtocols();
        factory.setExcludeCipherSuites();
        factory.setRenegotiationAllowed(false);
        factory.setUseCipherSuitesOrder(true);
        factory.setIncludeCipherSuites(cfg.ciphers().toArray(new String[0]));
        factory.setIncludeProtocols(cfg.protocols().toArray(new String[0]));
        customizer.setStsMaxAge(cfg.hstsMinAge());
        customizer.setStsIncludeSubDomains(false);
    }

    /**
     * @see #getMozSslConfigVariant(Config)
     */
    @NonNull
    public static MozillaSslConfigHelper.CONFIGURATION getMozSslConfigVariant(IServerInstance serverInstance) {
        return getMozSslConfigVariant(serverInstance.getAppConfig());
    }


    @NonNull
    public static MozillaSslConfigHelper.CONFIGURATION getMozSslConfigVariant(Config cfg) {
        return switch (cfg.getServer().getMozillaSslConfig().getConfiguration()) {
            case "modern" -> MozillaSslConfigHelper.CONFIGURATION.MODERN;
            case "intermediate" -> MozillaSslConfigHelper.CONFIGURATION.INTERMEDIATE;
            case "old" -> MozillaSslConfigHelper.CONFIGURATION.OLD;
            default -> throw new IllegalStateException(
                    "Unexpected value: " + cfg.getServer().getMozillaSslConfig().getConfiguration()
                            + " must be one of modern, intermediate or old (must be specified in lowercase, this is case sensitive)");
        };
    }
}
