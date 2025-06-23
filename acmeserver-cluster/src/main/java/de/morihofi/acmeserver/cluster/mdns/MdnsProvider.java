package de.morihofi.acmeserver.cluster.mdns;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

/**
 * Service discovery provider for mDNS operations.
 */
public interface MdnsProvider {
    /**
     * Start advertising the local instance and discover peers.
     *
     * @param instanceId unique instance ID
     * @param port       gRPC port
     * @param listener   callback receiving address and instanceId of peers
     */
    void start(String instanceId, int port, BiConsumer<InetSocketAddress, String> listener) throws IOException;

    /** Stop any advertisement or discovery. */
    void stop() throws IOException;
}
