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
package de.morihofi.acmeserver.config.network;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.io.Serializable;

/**
 * Represents the network configuration for the server, including DNS and Proxy settings.
 */
public class NetworkConfig implements Serializable {

    /**
     * DNS configuration for the server.
     */
    @ConfigurationField(name = "DNS")
    private DNSConfig dnsConfig = new DNSConfig();

    /**
     * Proxy configuration for the server.
     */
    @ConfigurationField(name = "Proxy")
    private ProxyConfig proxy = new ProxyConfig();

    /**
     * Gets the DNS configuration for the server.
     *
     * @return The current DNS configuration.
     */
    public DNSConfig getDnsConfig() {
        return dnsConfig;
    }

    /**
     * Sets the DNS configuration for the server.
     *
     * @param dnsConfig The new DNS configuration to set.
     */
    public void setDnsConfig(DNSConfig dnsConfig) {
        this.dnsConfig = dnsConfig;
    }

    /**
     * Gets the Proxy configuration for the server.
     *
     * @return The current Proxy configuration.
     */
    public ProxyConfig getProxy() {
        return proxy;
    }

    /**
     * Sets the Proxy configuration for the server.
     *
     * @param proxy The new Proxy configuration to set.
     */
    public void setProxy(ProxyConfig proxy) {
        this.proxy = proxy;
    }
}
