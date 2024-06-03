/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.config.network;

import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;

import java.util.ArrayList;
import java.util.List;

public class DNSConfig {

    @ConfigurationField(name = "DNS Servers (if empty use system DNS)")
    private List<String> dnsServers = new ArrayList<>(List.of("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"));

    @ConfigurationField(name = "Enable DNS over HTTPS")
    private Boolean dohEnabled = false;

    @ConfigurationField(name = "DNS over HTTP Endpoint")
    private String dohEndpoint = "https://dns.google/resolve";

    public List<String> getDnsServers() {
        return dnsServers;
    }

    public void setDnsServers(List<String> dnsServers) {
        this.dnsServers = dnsServers;
    }

    public Boolean getDohEnabled() {
        return dohEnabled;
    }

    public void setDohEnabled(Boolean dohEnabled) {
        this.dohEnabled = dohEnabled;
    }

    public String getDohEndpoint() {
        return dohEndpoint;
    }

    public void setDohEndpoint(String dohEndpoint) {
        this.dohEndpoint = dohEndpoint;
    }
}
