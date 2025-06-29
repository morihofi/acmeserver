/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
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
