/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.intf.network;

import de.morihofi.certgine.types.intf.network.dns.IDoHClient;

import java.util.List;

/**
 * NetworkClient is responsible for configuring and managing the OkHttpClient instance, including DNS settings and proxy configuration.
 */
public interface INetworkClient {
    /**
     * OkHttpClient instance for handling network requests.
     */
    okhttp3.OkHttpClient getOkHttpClient();

    /**
     * DoHClient instance for handling DNS over HTTPS requests.
     */
    IDoHClient getDoHClient();

    /**
     * unmodifiable List of DNS servers configured for the network client.
     */
    List<String> getDnsServer();

}
