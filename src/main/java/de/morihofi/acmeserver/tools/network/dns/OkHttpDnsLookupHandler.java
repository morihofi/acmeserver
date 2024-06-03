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

public class OkHttpDnsLookupHandler implements Dns {

    private final DoHClient doHClient;
    private final DNSConfig dnsConfig;

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    public OkHttpDnsLookupHandler(DoHClient doHClient, DNSConfig dnsConfig) {
        this.doHClient = doHClient;
        this.dnsConfig = dnsConfig;
    }

    @NotNull
    @Override
    public List<InetAddress> lookup(@NotNull String hostname) throws UnknownHostException {
        if(dnsConfig.getDohEnabled()){
            // Use DoH
            LOG.info("Lookup for {} using DNS over HTTPS", hostname);
            return DNSLookup.lookupHostnameUsingDoH(hostname, doHClient);
        }

        if(!dnsConfig.getDnsServers().isEmpty()){
            // Use configured Servers
            LOG.info("Lookup for {} using configured DNS Servers", hostname);
            return DNSLookup.lookupHostnameUsingDnsServerList(hostname, dnsConfig.getDnsServers());
        }

        // Otherwise use system configured DNS
        LOG.info("Lookup for {} using System DNS", hostname);
        return Arrays.asList(InetAddress.getAllByName(hostname));
    }
}
