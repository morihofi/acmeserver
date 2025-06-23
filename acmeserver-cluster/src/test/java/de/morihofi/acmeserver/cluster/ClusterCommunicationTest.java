package de.morihofi.acmeserver.cluster;

import de.morihofi.acmeserver.types.config.GrpcConfig;
import de.morihofi.acmeserver.types.events.EventSubscriber;
import de.morihofi.acmeserver.types.events.AbstractEvent;
import de.morihofi.acmeserver.types.events.ServerStartedEvent;
import de.morihofi.acmeserver.cluster.mdns.MockMdnsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ClusterCommunicationTest {
    private ClusterManager a;
    private ClusterManager b;
    private DistributedEventBus busA;
    private DistributedEventBus busB;
    private MockMdnsProvider mdns;

    @BeforeEach
    void setup() throws Exception {
        GrpcConfig confA = new GrpcConfig();
        confA.setEnabled(true);
        confA.setPort(60051);

        GrpcConfig confB = new GrpcConfig();
        confB.setEnabled(true);
        confB.setPort(60052);

        mdns = new MockMdnsProvider();

        busA = new DistributedEventBus();
        busB = new DistributedEventBus();
        a = new ClusterManager(confA, busA, mdns);
        b = new ClusterManager(confB, busB, mdns);
        busA.setClusterManager(a);
        busB.setClusterManager(b);
        // wait a bit for servers
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() {
        a.shutdown();
        b.shutdown();
        mdns = null;
    }

    static class CounterSubscriber implements EventSubscriber {
        final AtomicInteger count = new AtomicInteger();
        @Override
        public List<Class<? extends AbstractEvent>> canHandle() {
            return List.of(ServerStartedEvent.class);
        }
        @Override
        public void onEvent(AbstractEvent event) {
            count.incrementAndGet();
        }
    }

    @Test
    void testEventPropagation() throws InterruptedException {
        CounterSubscriber sub = new CounterSubscriber();
        busB.register(sub);
        busA.publish(new ServerStartedEvent(null));
        Thread.sleep(1000);
        assertEquals(1, sub.count.get());
    }

    @Test
    void testLeaderElection() {
        assertNotEquals(a.isLeader(), b.isLeader());
    }
}
