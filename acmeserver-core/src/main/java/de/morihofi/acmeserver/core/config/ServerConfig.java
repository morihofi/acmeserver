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

package de.morihofi.acmeserver.core.config;

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
     * Indicates whether SNI (Server Name Indication) checking is enabled for HTTPS hostname.
     */
    private boolean enableSniCheck = true;

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
