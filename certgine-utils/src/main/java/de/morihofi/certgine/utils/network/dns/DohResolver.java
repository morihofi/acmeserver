package de.morihofi.certgine.utils.network.dns;

import de.morihofi.certgine.types.intf.network.dns.IDoHClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolver strategy that performs DNS queries via DNS over HTTPS (DoH).
 *
 * <p>The resolver builds a DNS {@link Message} for the requested host and record type and delegates
 * the lookup to the configured {@link IDoHClient}. Only {@link ARecord} and {@link AAAARecord}
 * entries from the DoH response are translated to {@link InetAddress} instances.</p>
 */
@Slf4j
@AllArgsConstructor
public class DohResolver implements ResolverStrategy {

    private final IDoHClient doHClient;

    /**
     * Resolves the given host name using DNS over HTTPS.
     *
     * <p>A DNS query {@link Message} is constructed for the supplied record type and submitted to
     * the configured {@link IDoHClient}. The client is expected to return any matching DNS
     * {@link Record}s. Only {@link ARecord} and {@link AAAARecord} entries are converted to
     * {@link InetAddress} objects. Other record types are ignored. If the query fails or yields no
     * relevant results, an empty list is returned.</p>
     *
     * @param host the host name to resolve (a trailing dot is automatically appended if missing)
     * @param recordType the DNS record type (e.g., {@link org.xbill.DNS.Type#A},
     *                   {@link org.xbill.DNS.Type#AAAA})
     * @return list of IP addresses from A/AAAA records in the response; empty if none or on error
     */
    @Override
    public List<InetAddress> resolve(String host, int recordType) {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            Message query = Message.newQuery(Record.newRecord(Name.fromString(host + "."), recordType, DClass.IN));
            List<Record> records = doHClient.query(query);
            for (Record dnsRecord : records) {
                if (dnsRecord instanceof ARecord aRecord) {
                    addresses.add(aRecord.getAddress());
                } else if (dnsRecord instanceof AAAARecord aaaaRecord) {
                    addresses.add(aaaaRecord.getAddress());
                }
            }
        } catch (Exception e) {
            log.error("Error looking up {} using DoH", host, e);
        }
        return addresses;
    }
}
