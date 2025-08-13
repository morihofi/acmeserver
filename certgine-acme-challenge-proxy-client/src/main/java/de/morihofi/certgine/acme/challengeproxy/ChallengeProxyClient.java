/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */
package de.morihofi.certgine.acme.challengeproxy;


import de.morihofi.certgine.rpc.proto.acme.challenge.ChallengeCheckerGrpc;
import de.morihofi.certgine.rpc.proto.acme.challenge.Challenges;
import de.morihofi.certgine.types.config.ChallengeProxyClientConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for communicating with ACME challenge proxies using gRPC over mTLS.
 */
@Slf4j
public class ChallengeProxyClient {
    private final List<ManagedChannel> channels = new ArrayList<>();

    ChallengeProxyClient(List<ManagedChannel> channels) {
        this.channels.addAll(channels);
    }

    /**
     * Dispatches an HTTP challenge request to all configured proxies.
     *
     * @param request HTTP challenge request
     * @return list of responses from proxies
     */
    public List<Challenges.ChallengeResponse> dispatchHttp(Challenges.HttpChallengeRequest request) {
        List<Challenges.ChallengeResponse> results = new ArrayList<>();
        for (ManagedChannel channel : channels) {
            ChallengeCheckerGrpc.ChallengeCheckerBlockingStub stub = ChallengeCheckerGrpc.newBlockingStub(channel);
            results.add(stub.checkHttpChallenge(request));
        }
        return results;
    }

    /**
     * Dispatches a DNS challenge request to all configured proxies.
     *
     * @param request DNS challenge request
     * @return list of responses from proxies
     */
    public List<Challenges.ChallengeResponse> dispatchDns(Challenges.DnsChallengeRequest request) {
        List<Challenges.ChallengeResponse> results = new ArrayList<>();
        for (ManagedChannel channel : channels) {
            ChallengeCheckerGrpc.ChallengeCheckerBlockingStub stub = ChallengeCheckerGrpc.newBlockingStub(channel);
            results.add(stub.checkDnsChallenge(request));
        }
        return results;
    }

    /**
     * Closes all gRPC channels.
     */
    public void shutdown() {
        channels.forEach(ManagedChannel::shutdown);
    }
}
