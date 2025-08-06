/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.dns;

import de.morihofi.certgine.types.intf.network.dns.IDoHClient;
import lombok.extern.slf4j.Slf4j;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DNSLookup is a utility class for performing DNS lookups using either a list of DNS servers or DNS over HTTPS (DoH).
 */
@Slf4j
public class DNSLookup {

    /**
     * Performs DNS lookups for the given hostname using the provided resolver strategy. The method performs both IPv4
     * (A record) and IPv6 (AAAA record) lookups.
     *
     * @param hostname the hostname to look up.
     * @param resolver the resolver strategy to use for lookups.
     * @return a list of {@link InetAddress} objects corresponding to the DNS records found.
     */
    public static List<InetAddress> lookupHostname(String hostname, ResolverStrategy resolver) {
        List<InetAddress> result = new ArrayList<>();
        for (int type : new int[]{Type.A, Type.AAAA}) {
            result.addAll(resolver.resolve(hostname, type));
        }
        return result;
    }

    /**
     * Helper method to perform DNS lookups using DoH.
     *
     * @param hostname  the hostname to look up.
     * @param type      the type of DNS record (e.g., A, AAAA, TXT).
     * @param doHClient the DoH client to use for the lookup.
     * @return a list of Strings representing the DNS record values found.
     */
    public static List<Record> performDoHLookup(String hostname, int type, IDoHClient doHClient) {
        try {
            log.info("Resolving {} of type {} using DNS over HTTPS", hostname, Type.string(type));

            if (!hostname.endsWith(".")) {
                hostname = hostname + ".";
            }

            // Create a DNS query message
            Message query = Message.newQuery(Record.newRecord(Name.fromString(hostname), type, DClass.IN));

            // Perform the DoH query
            return doHClient.query(query);
        } catch (Exception e) {
            log.error("Error looking up {} using DoH", hostname, e);
        }
        return Collections.emptyList();
    }

    /**
     * Helper method to perform DNS lookups using a list of DNS servers.
     *
     * @param hostname   the hostname to look up.
     * @param type       the type of DNS record (e.g., A, AAAA, TXT).
     * @param dnsServers the list of DNS servers to use for the lookup.
     * @return a list of Strings representing the DNS record values found.
     */
    public static List<Record> performDnsServerLookup(String hostname, int type, List<String> dnsServers) {
        if (dnsServers.isEmpty()) {

            if (!hostname.endsWith(".")) {
                hostname = hostname + ".";
            }


            // Use the default system resolver
            try {
                Lookup lookup = new Lookup(hostname, type);
                Record[] records = lookup.run();
                return records != null ? Arrays.stream(records).toList() : Collections.emptyList();
            } catch (Exception e) {
                log.error("Failed to query default system DNS resolver for {}: {}", hostname, e.getMessage(), e);
                return Collections.emptyList();
            }
        }

        for (String dnsServer : dnsServers) {
            try {
                Lookup lookup = new Lookup(hostname, type);
                Resolver resolver = new SimpleResolver(dnsServer);
                lookup.setResolver(resolver);

                Record[] records = lookup.run();
                if (records != null) {
                    return Arrays.stream(records).toList();
                }
            } catch (Exception e) {
                log.error("Failed to query DNS server {}: {}", dnsServer, e.getMessage(), e);
                // Continue to the next DNS server if an exception occurs
            }
        }
        return Collections.emptyList();
    }
}
