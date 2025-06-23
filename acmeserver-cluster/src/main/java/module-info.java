module acmeserver.cluster {
    requires acmeserver.types;
    requires com.google.gson;
    requires io.grpc;
    requires io.grpc.netty.shaded;
    requires io.grpc.stub;
    requires javax.jmdns;
    requires org.slf4j;
}