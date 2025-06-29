/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.io.Serializable;

/**
 * Represents configuration parameters for a server, including DNS name and port settings.
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ServerConfig implements Serializable {

    /**
     * The DNS name that resolves to this server.
     */
    private String dnsName = "example.com";

    /**
     * The port configuration for the server.
     */
    private Ports ports = new Ports();

    /**
     * The directory path for HTTP logging.
     */
    private String loggingDirectory = null;

    /**
     * The Mozilla SSL configuration settings.
     */
    private MozillaSslConfig mozillaSslConfig = new MozillaSslConfig();

    /**
     * The advanced SSL/TLS configuration settings.
     */
    private SslServerConfig sslServerConfig = new SslServerConfig();

}
