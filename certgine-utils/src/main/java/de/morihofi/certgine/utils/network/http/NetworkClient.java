/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.certgine.utils.network.http;

import de.morihofi.certgine.types.config.network.NetworkConfig;
import de.morihofi.certgine.types.intf.network.INetworkClient;
import de.morihofi.certgine.utils.network.dns.OkHttpDnsLookupHandler;
import de.morihofi.certgine.utils.network.dns.internal.DoHClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
        this.doHClient = new DoHClient(networkConfig.getDnsConfig().getDohEndpoint(), this);
        this.okHttpClient = new OkHttpClient.Builder()
                .dns(new OkHttpDnsLookupHandler(doHClient, networkConfig.getDnsConfig()))
                .proxy(getProxy())
                .build();
    }

    public List<String> getDnsServer() {
        return Collections.unmodifiableList(dnsServer);
    }

    public Proxy getProxy() {
        Proxy.Type proxyType = switch (networkConfig.getProxy().getType()) {
            case "socks", "socks4", "socks5" -> Proxy.Type.SOCKS;
            case "http" -> Proxy.Type.HTTP;
            default -> Proxy.Type.DIRECT;
        };
        Proxy proxy = Proxy.NO_PROXY;

        try {
            int proxyPort = networkConfig.getProxy().getPort();
            String proxyHost = networkConfig.getProxy().getHost();

            if (networkConfig.getProxy().getEnabled()) {
                SocketAddress socketAddress = new InetSocketAddress(proxyHost, proxyPort);
                proxy = new Proxy(proxyType, socketAddress);
            }

            if (networkConfig.getProxy().getAuthentication().isEnabled()) {
                String proxyUser = networkConfig.getProxy().getAuthentication().getUsername();
                String proxyPassword = networkConfig.getProxy().getAuthentication().getPassword();

                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(proxyHost) && proxyPort == getRequestingPort()) {
                            return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                        }
                        return null;
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Failed to initialize proxy configuration, returning a no-proxy configuration", ex);
            return Proxy.NO_PROXY;
        }

        return proxy;
    }
}
