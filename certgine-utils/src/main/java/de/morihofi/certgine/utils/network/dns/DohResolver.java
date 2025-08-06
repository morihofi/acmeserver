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
 * Resolver strategy that performs DNS queries via DNS over HTTPS.
 */
@Slf4j
@AllArgsConstructor
public class DohResolver implements ResolverStrategy {

    private final IDoHClient doHClient;

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
