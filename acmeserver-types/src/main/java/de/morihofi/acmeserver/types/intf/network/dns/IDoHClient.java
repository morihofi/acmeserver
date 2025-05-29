package de.morihofi.acmeserver.types.intf.network.dns;

import java.io.IOException;
import java.util.List;

public interface IDoHClient {
    List<org.xbill.DNS.Record> query(org.xbill.DNS.Message query) throws IOException;

}
