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

package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * Represents configuration parameters for a server, including DNS name and port settings.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ServerConfig implements Serializable {
    @ConfigurationField(name = "DNS Name that resolves to this Server", required = true)
    private String dnsName = "example.com";
    @ConfigurationField(name = "Ports")
    private Ports ports = new Ports();
    @ConfigurationField(name = "Enable SNI Checking (HTTPS Hostname)")
    private boolean enableSniCheck = true;
    @ConfigurationField(name = "HTTP Logging directory path")
    private String loggingDirectory = null;
    @ConfigurationField(name = "Mozilla SSL-Config settings")
    private MozillaSslConfig mozillaSslConfig = new MozillaSslConfig();
    @ConfigurationField(name = "Advanced SSL/TLS Configuration settings")
    private SslServerConfig sslServerConfig = new SslServerConfig();

    /**
     * Get the DNS name of the server.
     *
     * @return The DNS name.
     */
    public String getDnsName() {
        return dnsName;
    }

    /**
     * Set the DNS name of the server.
     *
     * @param dnsName The DNS name to set.
     */
    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    /**
     * Get the port configuration for the server.
     *
     * @return The port configuration.
     */
    public Ports getPorts() {
        return this.ports;
    }

    /**
     * Set the port configuration for the server.
     *
     * @param ports The port configuration to set.
     */

    public void setPorts(Ports ports) {
        this.ports = ports;
    }

    public boolean isEnableSniCheck() {
        return enableSniCheck;
    }

    public void setEnableSniCheck(boolean enableSniCheck) {
        this.enableSniCheck = enableSniCheck;
    }

    public String getLoggingDirectory() {
        return loggingDirectory;
    }

    public void setLoggingDirectory(String loggingDirectory) {
        this.loggingDirectory = loggingDirectory;
    }

    public MozillaSslConfig getMozillaSslConfig() {
        return mozillaSslConfig;
    }

    public void setMozillaSslConfig(MozillaSslConfig mozillaSslConfig) {
        this.mozillaSslConfig = mozillaSslConfig;
    }

    public SslServerConfig getSslServerConfig() {
        return sslServerConfig;
    }

    public void setSslServerConfig(SslServerConfig sslServerConfig) {
        this.sslServerConfig = sslServerConfig;
    }
}
