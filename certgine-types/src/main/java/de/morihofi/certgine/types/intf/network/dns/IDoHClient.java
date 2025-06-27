package de.morihofi.certgine.types.intf.network.dns;

import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

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
    List<Record> query(Message query) throws IOException;

}
