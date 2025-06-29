/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config.network;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents the network configuration for the server, including DNS and Proxy settings.
 */
@Data
public class NetworkConfig implements Serializable {

    /**
     * DNS configuration for the server.
     */
    private DNSConfig dnsConfig = new DNSConfig();

    /**
     * Proxy configuration for the server.
     */
    private ProxyConfig proxy = new ProxyConfig();

}
