/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.dns;

import de.morihofi.certgine.types.config.network.DNSConfig;
import de.morihofi.certgine.utils.network.dns.internal.DoHClient;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The OkHttpDnsLookupHandler class is responsible for handling DNS lookups using the OkHttp library.
 * It supports DNS over HTTPS (DoH) and custom DNS servers based on the provided configuration.
 */
@Slf4j
@AllArgsConstructor
public class OkHttpDnsLookupHandler implements Dns {

    /**
     * The DoHClient instance used for performing DNS over HTTPS lookups.
     */
    private final DoHClient doHClient;

    /**
     * The DNS configuration settings.
     */
    private final DNSConfig dnsConfig;


    /**
     * Performs a DNS lookup for the specified hostname.
     * This method supports DNS over HTTPS (DoH), custom DNS servers, and system DNS based on the provided configuration.
     *
     * @param hostname The hostname to look up.
     * @return A list of InetAddress instances representing the resolved IP addresses.
     * @throws UnknownHostException If the hostname cannot be resolved.
     */
    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        if (hostname.equals("localhost")) {
            return Collections.singletonList(InetAddress.getLocalHost());
        }

        if (dnsConfig.getDohEnabled()) {
            // Use DoH
            log.info("Lookup for {} using DNS over HTTPS (DoH)", hostname);
            return DNSLookup.lookupHostname(hostname, new DohResolver(doHClient));
        }

        if (!dnsConfig.getDnsServers().isEmpty()) {
            // Use configured Servers
            log.info("Lookup for {} using configured DNS Servers", hostname);
            return DNSLookup.lookupHostname(hostname, new DnsServerResolver(dnsConfig.getDnsServers()));
        }

        // Otherwise use system configured DNS
        log.info("Lookup for {} using System DNS", hostname);
        return Arrays.asList(InetAddress.getAllByName(hostname));
    }
}
