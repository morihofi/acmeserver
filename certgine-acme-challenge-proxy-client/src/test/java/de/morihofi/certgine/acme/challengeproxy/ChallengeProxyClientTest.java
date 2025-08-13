/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */
package de.morihofi.certgine.acme.challengeproxy;

import de.morihofi.certgine.rpc.proto.acme.challenge.*;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeProxyClientTest {
    private io.grpc.Server server;
    private ManagedChannel channel;
    private ChallengeProxyClient client;

    @BeforeEach
    void setup() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new ChallengeCheckerGrpc.ChallengeCheckerImplBase() {
                    @Override
                    public void checkHttpChallenge(Challenges.HttpChallengeRequest request, StreamObserver<Challenges.ChallengeResponse> responseObserver) {
                        responseObserver.onNext(Challenges.ChallengeResponse.newBuilder().setOk(true).build());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void checkDnsChallenge(Challenges.DnsChallengeRequest request, StreamObserver<Challenges.ChallengeResponse> responseObserver) {
                        responseObserver.onNext(Challenges.ChallengeResponse.newBuilder().setOk(true).build());
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = new ChallengeProxyClient(List.of(channel));
    }

    @AfterEach
    void tearDown() {
        client.shutdown();
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void dispatchHttp() {
        Challenges.HttpChallengeRequest req = Challenges.HttpChallengeRequest.newBuilder().build();
        List<Challenges.ChallengeResponse> res = client.dispatchHttp(req);
        assertEquals(1, res.size());
        assertTrue(res.getFirst().getOk());
    }

    @Test
    void dispatchDns() {
        Challenges.DnsChallengeRequest req = Challenges.DnsChallengeRequest.newBuilder().build();
        List<Challenges.ChallengeResponse> res = client.dispatchDns(req);
        assertEquals(1, res.size());
        assertTrue(res.getFirst().getOk());
    }
}
