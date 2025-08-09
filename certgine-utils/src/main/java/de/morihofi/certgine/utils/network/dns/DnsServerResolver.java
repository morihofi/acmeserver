package de.morihofi.certgine.utils.network.dns;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolver strategy that queries a list of DNS servers.
 */
@Slf4j
@AllArgsConstructor
public class DnsServerResolver implements ResolverStrategy {

    private final List<String> dnsServers;

    /**
     * Resolves the specified host name using the provided DNS record type against the configured DNS servers.
     *
     * @param host       the domain name to look up, such as {@code "example.com"}
     * @param recordType the DNS record type constant (for example {@link org.xbill.DNS.Type#A} or
     *                   {@link org.xbill.DNS.Type#AAAA}) that defines which addresses should be retrieved
     * @return a list of IP addresses returned by the DNS servers; the list will be empty if no records are found
     */
    @Override
    public List<InetAddress> resolve(String host, int recordType) {
        List<InetAddress> addresses = new ArrayList<>();
        for (String dnsServer : dnsServers) {
            try {
                Lookup lookup = new Lookup(host, recordType);
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
                    if (!addresses.isEmpty()) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to query DNS server {}: {}", dnsServer, e.getMessage(), e);
            }
        }
        return addresses;
    }
}
