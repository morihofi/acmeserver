/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.http;

import de.morihofi.certgine.types.config.network.NetworkConfig;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.utils.network.dns.OkHttpDnsLookupHandler;
import de.morihofi.certgine.utils.network.dns.internal.DoHClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Slf4j
public class NetworkClient implements INetworkClient {


    @Getter
    private final OkHttpClient okHttpClient;

    /**
     * DoHClient instance for handling DNS over HTTPS requests.
     */
    @Getter
    private final DoHClient doHClient;

    /**
     * Network configuration settings.
     */
    private final NetworkConfig networkConfig;

    private final List<String> dnsServer = new ArrayList<>();

    /**
     * Constructs a NetworkClient with the specified application configuration.
     *
     * @param networkConfig the application's network configuration.
     */
    public NetworkClient(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
        this.dnsServer.addAll(networkConfig.getDnsConfig().getDnsServers());

        Optional<Proxy> proxyOptional = getProxy();
        this.doHClient = new DoHClient(networkConfig.getDnsConfig().getDohEndpoint(), proxyOptional, this);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dns(new OkHttpDnsLookupHandler(doHClient, networkConfig.getDnsConfig()))
                .proxy(proxyOptional.orElse(Proxy.NO_PROXY));

        if (proxyOptional.isPresent() && networkConfig.getProxy().getAuthentication().isEnabled()) {
            String proxyUser = networkConfig.getProxy().getAuthentication().getUsername();
            String proxyPassword = networkConfig.getProxy().getAuthentication().getPassword();
            builder.proxyAuthenticator(new Authenticator() {
                @Override
                public okhttp3.Request authenticate(okhttp3.Route route, okhttp3.Response response) {
                    String credential = Credentials.basic(proxyUser, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                }
            });
        }

        this.okHttpClient = builder.build();
    }

    public List<String> getDnsServer() {
        return Collections.unmodifiableList(dnsServer);
    }

    private Optional<Proxy> getProxy() {
        try {
            if (!networkConfig.getProxy().getEnabled()) {
                return Optional.empty();
            }

            int proxyPort = networkConfig.getProxy().getPort();
            String proxyHost = networkConfig.getProxy().getHost();

            Optional<ProxyScheme> scheme = ProxyScheme.fromString(networkConfig.getProxy().getType());
            Proxy.Type proxyType = scheme.map(s -> switch (s) {
                case SOCKS -> Proxy.Type.SOCKS;
                case HTTP -> Proxy.Type.HTTP;
            }).orElse(Proxy.Type.DIRECT);

            SocketAddress socketAddress = new InetSocketAddress(proxyHost, proxyPort);
            return Optional.of(new Proxy(proxyType, socketAddress));
        } catch (Exception ex) {
            log.error("Failed to initialize proxy configuration, returning a no-proxy configuration", ex);
            return Optional.empty();
        }
    }
}
