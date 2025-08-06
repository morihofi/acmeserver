package de.morihofi.certgine.utils.network.dns;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DNSLookupTest {

    @Test
    void testLookupHostnameUsesStrategyForAllRecordTypes() throws Exception {
        ResolverStrategy strategy = Mockito.mock(ResolverStrategy.class);
        List<InetAddress> ipv4 = List.of(InetAddress.getByName("1.2.3.4"));
        List<InetAddress> ipv6 = List.of(InetAddress.getByName("::1"));
        when(strategy.resolve("example.com", Type.A)).thenReturn(ipv4);
        when(strategy.resolve("example.com", Type.AAAA)).thenReturn(ipv6);

        List<InetAddress> result = DNSLookup.lookupHostname("example.com", strategy);

        assertTrue(result.containsAll(ipv4));
        assertTrue(result.containsAll(ipv6));
        verify(strategy).resolve("example.com", Type.A);
        verify(strategy).resolve("example.com", Type.AAAA);
    }
}
