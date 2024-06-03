/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.morihofi.acmeserver.tools.network;

import de.morihofi.acmeserver.config.network.NetworkConfig;
import de.morihofi.acmeserver.tools.network.dns.OkHttpDnsLookupHandler;
import de.morihofi.acmeserver.tools.network.dns.internal.DoHClient;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NetworkClient is responsible for configuring and managing the OkHttpClient instance, including DNS settings and proxy configuration.
 */
public class NetworkClient {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final OkHttpClient client;
    private final DoHClient doHClient;
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
        this.client = new OkHttpClient.Builder()
                .dns(new OkHttpDnsLookupHandler(doHClient, networkConfig.getDnsConfig()))
                .proxy(getProxy())
                .build();
    }

    /**
     * Returns the configured OkHttpClient instance.
     *
     * @return the OkHttpClient instance.
     */
    public OkHttpClient getOkHttpClient() {
        return client;
    }

    /**
     * Returns the configured DoH client
     *
     * @return the DoH client instance.
     */
    public DoHClient getDoHClient() {
        return doHClient;
    }

    /**
     * Returns an unmodifiable list of DNS servers configured in the application.
     *
     * @return a list of DNS servers.
     */
    public List<String> getDnsServer() {
        return Collections.unmodifiableList(dnsServer);
    }

    /**
     * Retrieves and configures Proxy based on application settings. The method configures the proxy settings and authentication details, if
     * required.
     *
     * @return A Proxy object configured based on application settings.
     */
    public Proxy getProxy() {
        Proxy.Type proxyType = switch (networkConfig.getProxy().getType()) {
            case "socks" -> Proxy.Type.SOCKS;
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
            LOG.error("Failed to initialize proxy configuration", ex);
        }

        return proxy;
    }
}
