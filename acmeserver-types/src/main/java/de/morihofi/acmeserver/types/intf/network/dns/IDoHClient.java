package de.morihofi.acmeserver.types.intf.network.dns;

import java.io.IOException;
import java.util.List;

public interface IDoHClient {
    /**
     * Queries the DNS server using DNS over HTTPS (DoH) protocol.
     *
     * @param query the DNS query message to be sent
     * @return a list of DNS records in the response
     * @throws IOException if an error occurs while sending the query or receiving the response
     */
    List<org.xbill.DNS.Record> query(org.xbill.DNS.Message query) throws IOException;

}
