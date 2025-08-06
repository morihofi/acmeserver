package de.morihofi.certgine.utils.network.dns;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DnsServerResolverTest {

    @Test
    void testResolveReturnsAddresses() throws Exception {
        Name name = Name.fromString("example.com.");
        ARecord aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("1.2.3.4"));
        AAAARecord aaaaRecord = new AAAARecord(name, DClass.IN, 0, InetAddress.getByName("::1"));

        try (MockedConstruction<Lookup> mocked = Mockito.mockConstruction(Lookup.class, (mock, context) -> {
            int type = (Integer) context.arguments().get(1);
            if (type == Type.A) {
                when(mock.run()).thenReturn(new Record[]{aRecord});
            } else {
                when(mock.run()).thenReturn(new Record[]{aaaaRecord});
            }
        })) {
            DnsServerResolver resolver = new DnsServerResolver(List.of("1.1.1.1"));
            List<InetAddress> ipv4 = resolver.resolve("example.com", Type.A);
            List<InetAddress> ipv6 = resolver.resolve("example.com", Type.AAAA);

            assertEquals(List.of(InetAddress.getByName("1.2.3.4")), ipv4);
            assertEquals(List.of(InetAddress.getByName("::1")), ipv6);
        }
    }
}
