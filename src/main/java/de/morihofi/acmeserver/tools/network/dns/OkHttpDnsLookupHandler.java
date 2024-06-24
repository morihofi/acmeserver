/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.tools.network.dns;

import de.morihofi.acmeserver.config.network.DNSConfig;
import de.morihofi.acmeserver.tools.network.dns.internal.DoHClient;
import okhttp3.Dns;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * The OkHttpDnsLookupHandler class is responsible for handling DNS lookups using the OkHttp library.
 * It supports DNS over HTTPS (DoH) and custom DNS servers based on the provided configuration.
 */
public class OkHttpDnsLookupHandler implements Dns {

    /**
     * Logger for the OkHttpDnsLookupHandler class.
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The DoHClient instance used for performing DNS over HTTPS lookups.
     */
    private final DoHClient doHClient;

    /**
     * The DNS configuration settings.
     */
    private final DNSConfig dnsConfig;

    /**
     * Constructs a new OkHttpDnsLookupHandler instance with the specified DoHClient and DNS configuration.
     *
     * @param doHClient The DoHClient instance used for performing DNS over HTTPS lookups.
     * @param dnsConfig The DNS configuration settings.
     */
    public OkHttpDnsLookupHandler(DoHClient doHClient, DNSConfig dnsConfig) {
        this.doHClient = doHClient;
        this.dnsConfig = dnsConfig;
    }

    /**
     * Performs a DNS lookup for the specified hostname.
     * This method supports DNS over HTTPS (DoH), custom DNS servers, and system DNS based on the provided configuration.
     *
     * @param hostname The hostname to look up.
     * @return A list of InetAddress instances representing the resolved IP addresses.
     * @throws UnknownHostException If the hostname cannot be resolved.
     */
    @NotNull
    @Override
    public List<InetAddress> lookup(@NotNull String hostname) throws UnknownHostException {
        if (dnsConfig.getDohEnabled()) {
            // Use DoH
            LOG.info("Lookup for {} using DNS over HTTPS", hostname);
            return DNSLookup.lookupHostnameUsingDoH(hostname, doHClient);
        }

        if (!dnsConfig.getDnsServers().isEmpty()) {
            // Use configured Servers
            LOG.info("Lookup for {} using configured DNS Servers", hostname);
            return DNSLookup.lookupHostnameUsingDnsServerList(hostname, dnsConfig.getDnsServers());
        }

        // Otherwise use system configured DNS
        LOG.info("Lookup for {} using System DNS", hostname);
        return Arrays.asList(InetAddress.getAllByName(hostname));
    }
}
