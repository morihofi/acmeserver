package de.morihofi.acmeserver.cluster;

import de.morihofi.acmeserver.cluster.proto.EventMessage;
import de.morihofi.acmeserver.cluster.proto.Empty;
import de.morihofi.acmeserver.cluster.proto.InstanceInfo;
import de.morihofi.acmeserver.cluster.mdns.JmDnsProvider;
import de.morihofi.acmeserver.cluster.mdns.MdnsProvider;
import de.morihofi.acmeserver.types.config.GrpcConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.CallOptions;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages gRPC communication between cluster members.
 */
public class ClusterManager {
    private final DistributedEventBus eventBus;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final Set<String> peerIds = ConcurrentHashMap.newKeySet();
    private final String instanceId;
    private final MdnsProvider mdns;
    private Server server;
    private boolean leader = true;
    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class);

    public boolean isLeader() {
        return leader;
    }

    public ClusterManager(GrpcConfig config, DistributedEventBus bus) throws IOException {
        this(config, bus, new JmDnsProvider());
    }

    public ClusterManager(GrpcConfig config, DistributedEventBus bus, MdnsProvider mdns) throws IOException {
        this.eventBus = bus;
        this.mdns = mdns;
        this.instanceId = UUID.randomUUID().toString();
        if (config.isEnabled()) {
            startServer(config.getPort());
            mdns.start(instanceId, config.getPort(), this::connectPeer);
            electLeader();
        }
    }

    private static final MethodDescriptor<EventMessage, Empty> SEND_EVENT = MethodDescriptor.<EventMessage, Empty>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("cluster.ClusterService/SendEvent")
            .setRequestMarshaller(new JsonMarshaller<>(EventMessage.class))
            .setResponseMarshaller(new JsonMarshaller<>(Empty.class))
            .build();

    private static final MethodDescriptor<Empty, InstanceInfo> GET_INSTANCE_INFO = MethodDescriptor.<Empty, InstanceInfo>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("cluster.ClusterService/GetInstanceInfo")
            .setRequestMarshaller(new JsonMarshaller<>(Empty.class))
            .setResponseMarshaller(new JsonMarshaller<>(InstanceInfo.class))
            .build();

    private void startServer(int port) throws IOException {
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder("cluster.ClusterService");
        new ClusterServiceImpl().bind(builder);
        server = NettyServerBuilder.forPort(port)
                .addService(builder.build())
                .build()
                .start();
        log.info("Started gRPC server on {}", port);
    }

    private void connectPeer(InetSocketAddress address, String peerId) {
        if (peerId.equals(instanceId) || channels.containsKey(peerId)) {
            return;
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(address.getHostString(), address.getPort())
                .usePlaintext()
                .build();
        channels.put(peerId, channel);
        peerIds.add(peerId);
        electLeader();
    }

    private void electLeader() {
        List<String> ids = new ArrayList<>(peerIds);
        ids.add(instanceId);
        leader = instanceId.equals(Collections.min(ids));
        log.info("Leader election result: {}", leader);
    }

    public void broadcastEvent(String type, String json) {
        EventMessage msg = new EventMessage(type, json);
        for (ManagedChannel channel : channels.values()) {
            try {
                ClientCalls.blockingUnaryCall(channel, SEND_EVENT, CallOptions.DEFAULT, msg);
            } catch (Exception ex) {
                log.warn("Failed to send event to peer", ex);
            }
        }
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown();
        }
        channels.values().forEach(ManagedChannel::shutdown);
        try {
            mdns.stop();
        } catch (IOException e) {
            log.warn("Failed to stop mDNS", e);
        }
    }

    private class ClusterServiceImpl {
        void bind(ServerServiceDefinition.Builder builder) {
            builder.addMethod(SEND_EVENT, ServerCalls.asyncUnaryCall((req, obs) -> {
                eventBus.publishFromCluster(req.getType(), req.getJson());
                obs.onNext(Empty.INSTANCE);
                obs.onCompleted();
            }));
            builder.addMethod(GET_INSTANCE_INFO, ServerCalls.asyncUnaryCall((req, obs) -> {
                obs.onNext(new InstanceInfo(instanceId));
                obs.onCompleted();
            }));
        }
    }
}
