package de.morihofi.acmeserver.cluster;

import com.google.gson.Gson;
import de.morihofi.acmeserver.types.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EventBus that optionally propagates events to remote peers via gRPC.
 */
public class DistributedEventBus extends EventBus {
    private final Gson gson = new Gson();
    private ClusterManager clusterManager;
    private static final Logger log = LoggerFactory.getLogger(DistributedEventBus.class);

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    /**
     * Publishes the given event locally and, if a cluster manager is present,
     * broadcasts it to all peers.
     *
     * @param event event to publish
     * @param <T>   type of event
     */
    @Override
    public <T> void publish(T event) {
        super.publish(event);
        if (clusterManager != null) {
            clusterManager.broadcastEvent(event.getClass().getName(), gson.toJson(event));
        }
    }

    void publishFromCluster(String type, String json) {
        try {
            Class<?> clazz = Class.forName(type);
            Object event = gson.fromJson(json, clazz);
            super.publish(event);
        } catch (ClassNotFoundException e) {
            log.warn("Received unknown event type {}", type);
        }
    }
}
