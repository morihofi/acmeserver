package de.morihofi.acmeserver.cluster.mdns;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

/**
 * mDNS discovery based on JmDNS.
 */
public class JmDnsProvider implements MdnsProvider {
    private static final String SERVICE_TYPE = "_acmeserver._tcp.local.";
    private JmDNS jmdns;
    private ServiceInfo serviceInfo;

    @Override
    public void start(String instanceId, int port, BiConsumer<InetSocketAddress, String> listener) throws IOException {
        jmdns = JmDNS.create(InetAddress.getLocalHost());
        serviceInfo = ServiceInfo.create(SERVICE_TYPE, instanceId, port, "");
        jmdns.registerService(serviceInfo);
        jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                jmdns.requestServiceInfo(event.getType(), event.getName());
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                if (!event.getName().equals(instanceId)) {
                    InetAddress[] addrs = event.getInfo().getInetAddresses();
                    if (addrs.length > 0) {
                        listener.accept(new InetSocketAddress(addrs[0], event.getInfo().getPort()), event.getName());
                    }
                }
            }
        });
    }

    @Override
    public void stop() throws IOException {
        if (jmdns != null) {
            if (serviceInfo != null) {
                jmdns.unregisterService(serviceInfo);
            }
            jmdns.close();
        }
    }
}
