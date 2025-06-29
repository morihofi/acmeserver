/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.network.ssl.mozillasslconfig.response.version5dot1up;

import lombok.Getter;

import java.util.List;

@Getter
public class Ciphers {
    private List<String> openssl;
    private List<String> go;
    private List<String> iana;
    private List<String> caddy;
}
