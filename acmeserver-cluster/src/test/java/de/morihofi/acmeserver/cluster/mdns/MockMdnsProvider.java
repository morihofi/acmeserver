package de.morihofi.acmeserver.cluster.mdns;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * In-memory mDNS mock used for unit tests.
 */
public class MockMdnsProvider implements MdnsProvider {
    private static final Map<String, Registration> REG = new HashMap<>();
    private String id;

    @Override
    public synchronized void start(String instanceId, int port, BiConsumer<InetSocketAddress, String> listener) {
        this.id = instanceId;
        REG.values().forEach(r -> {
            listener.accept(new InetSocketAddress("localhost", r.port), r.id);
            r.listener.accept(new InetSocketAddress("localhost", port), instanceId);
        });
        REG.put(instanceId, new Registration(port, instanceId, listener));
    }

    @Override
    public synchronized void stop() throws IOException {
        REG.remove(id);
    }

    private record Registration(int port, String id, BiConsumer<InetSocketAddress, String> listener) {}
}
