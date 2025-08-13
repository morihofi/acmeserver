/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */
package de.morihofi.certgine.types.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for ACME challenge proxies.
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ChallengeProxyClientConfig implements Serializable {
    /**
     * Enables usage of challenge proxies.
     */
    private boolean enabled = false;

    /**
     * List of proxy endpoints.
     */
    private List<Proxy> proxies = new ArrayList<>();

    /**
     * Single proxy endpoint configuration.
     */
    @Data
    public static class Proxy implements Serializable {
        /**
         * Hostname of the proxy.
         */
        private String host;
        /**
         * Port of the proxy.
         */
        private int port;
        /**
         * Path to CA bundle for validating the proxy certificate.
         */
        private String caBundle;
    }
}
