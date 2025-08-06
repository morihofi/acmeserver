package de.morihofi.certgine.utils.network.dns;

import de.morihofi.certgine.types.intf.network.dns.IDoHClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class DohResolverTest {

    @Test
    void testResolveReturnsAddresses() throws Exception {
        IDoHClient client = Mockito.mock(IDoHClient.class);
        Name name = Name.fromString("example.com.");
        ARecord aRecord = new ARecord(name, DClass.IN, 0, InetAddress.getByName("1.2.3.4"));
        AAAARecord aaaaRecord = new AAAARecord(name, DClass.IN, 0, InetAddress.getByName("::1"));

        Mockito.when(client.query(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            int type = msg.getQuestion().getType();
            if (type == Type.A) {
                return List.of(aRecord);
            }
            return List.of(aaaaRecord);
        });

        DohResolver resolver = new DohResolver(client);
        List<InetAddress> ipv4 = resolver.resolve("example.com", Type.A);
        List<InetAddress> ipv6 = resolver.resolve("example.com", Type.AAAA);

        assertEquals(List.of(InetAddress.getByName("1.2.3.4")), ipv4);
        assertEquals(List.of(InetAddress.getByName("::1")), ipv6);
    }
}
