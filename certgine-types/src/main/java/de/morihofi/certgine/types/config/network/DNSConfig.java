/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.config.network;


import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class DNSConfig implements Serializable {

    private List<String> dnsServers = new ArrayList<>(List.of("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"));
    private Boolean dohEnabled = false;
    private String dohEndpoint = "https://cloudflare-dns.com/dns-query";

}
