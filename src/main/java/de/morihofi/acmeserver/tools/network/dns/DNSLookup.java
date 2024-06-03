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

import de.morihofi.acmeserver.tools.network.dns.internal.DoHClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DNSLookup is a utility class for performing DNS lookups using either a list of DNS servers or DNS over HTTPS (DoH).
 */
public class DNSLookup {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Performs DNS lookups for the given hostname using a list of DNS servers. The method performs both IPv4 (A record) and IPv6 (AAAA
     * record) lookups.
     *
     * @param hostname   the hostname to look up.
     * @param dnsServers the list of DNS servers to use for the lookup.
     * @return a list of {@link InetAddress} objects corresponding to the DNS records found.
     */
    public static List<InetAddress> lookupHostnameUsingDnsServerList(String hostname, List<String> dnsServers) {
        List<InetAddress> result = new ArrayList<>();
        result.addAll(doHostnameLookup(hostname, Type.A, dnsServers));  // IPv4 lookup
        result.addAll(doHostnameLookup(hostname, Type.AAAA, dnsServers));  // IPv6 lookup

        return result;
    }

    /**
     * Performs DNS lookups for the given hostname using DNS over HTTPS (DoH). The method performs both IPv4 (A record) and IPv6 (AAAA
     * record) lookups.
     *
     * @param hostname  the hostname to look up.
     * @param doHClient the DoH client to use for the lookup.
     * @return a list of {@link InetAddress} objects corresponding to the DNS records found.
     */
    public static List<InetAddress> lookupHostnameUsingDoH(String hostname, DoHClient doHClient) {
        List<InetAddress> result = new ArrayList<>();
        result.addAll(doHostnameLookup(hostname, Type.A, doHClient));  // IPv4 lookup
        result.addAll(doHostnameLookup(hostname, Type.AAAA, doHClient));  // IPv6 lookup

        return result;
    }

    /**
     * Helper method to perform DNS lookups using DoH.
     *
     * @param hostname  the hostname to look up.
     * @param type      the type of DNS record (e.g., A, AAAA).
     * @param doHClient the DoH client to use for the lookup.
     * @return a list of {@link InetAddress} objects corresponding to the DNS records found.
     */
    public static List<InetAddress> doHostnameLookup(String hostname, int type, DoHClient doHClient) {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            LOG.info("Resolving {} of type {} using DNS over HTTPS", hostname, Type.string(type));

            // Create a DNS query message
            Message query = Message.newQuery(Record.newRecord(Name.fromString(hostname + "."), type, DClass.IN));

            // Perform the DoH query
            List<Record> records = doHClient.query(query);

            // Process the response records
            for (Record dnsRecord : records) {
                if (dnsRecord instanceof ARecord aRecord) {
                    addresses.add(aRecord.getAddress());
                } else if (dnsRecord instanceof AAAARecord aaaaRecord) {
                    addresses.add(aaaaRecord.getAddress());
                }
            }
        } catch (Exception e) {
            LOG.error("Error looking up {} using DoH", hostname, e);
        }
        return addresses;
    }

    /**
     * Helper method to perform DNS lookups using DoH.
     *
     * @param hostname  the hostname to look up.
     * @param type      the type of DNS record (e.g., A, AAAA, TXT).
     * @param doHClient the DoH client to use for the lookup.
     * @return a list of Strings representing the DNS record values found.
     */
    public static List<Record> performDoHLookup(String hostname, int type, DoHClient doHClient) {
        try {
            LOG.info("Resolving {} of type {} using DNS over HTTPS", hostname, Type.string(type));

            // Create a DNS query message
            Message query = Message.newQuery(Record.newRecord(Name.fromString(hostname), type, DClass.IN));

            // Perform the DoH query
            return doHClient.query(query);
        } catch (Exception e) {
            LOG.error("Error looking up {} using DoH", hostname, e);
        }
        return Collections.emptyList();
    }

    /**
     * Helper method to perform DNS lookups using a list of DNS servers.
     *
     * @param hostname   the hostname to look up.
     * @param type       the type of DNS record (e.g., A, AAAA).
     * @param dnsServers the list of DNS servers to use for the lookup.
     * @return a list of {@link InetAddress} objects corresponding to the DNS records found.
     */
    public static List<InetAddress> doHostnameLookup(String hostname, int type, List<String> dnsServers) {
        List<InetAddress> addresses = new ArrayList<>();
        for (String dnsServer : dnsServers) {
            try {
                Lookup lookup = new Lookup(hostname, type);
                Resolver resolver = new SimpleResolver(dnsServer);
                lookup.setResolver(resolver);
                Record[] records = lookup.run();
                if (records != null) {
                    for (Record dnsRecord : records) {
                        if (dnsRecord instanceof ARecord aRecord) {
                            addresses.add(aRecord.getAddress());
                        } else if (dnsRecord instanceof AAAARecord aaaaRecord) {
                            addresses.add(aaaaRecord.getAddress());
                        }
                    }
                    // If records are found, break out of the loop
                    if (!addresses.isEmpty()) {
                        break;
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed to query DNS server {}: {}", dnsServer, e.getMessage(), e);
                // Continue to the next DNS server if an exception occurs
            }
        }
        return addresses;
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
            // Use the default system resolver
            try {
                Lookup lookup = new Lookup(hostname, type);
                Record[] records = lookup.run();
                return records != null ? Arrays.stream(records).toList() : Collections.emptyList();
            } catch (Exception e) {
                LOG.error("Failed to query default system DNS resolver for {}: {}", hostname, e.getMessage(), e);
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
                LOG.error("Failed to query DNS server {}: {}", dnsServer, e.getMessage(), e);
                // Continue to the next DNS server if an exception occurs
            }
        }
        return Collections.emptyList();
    }
}
