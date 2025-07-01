/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.ui.frontend.modern;

import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.ReverseProxyServlet;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServletMount(servletMountPoint = "/*", protect = true)
public class WebUiServlet extends ReverseProxyServlet {
    public WebUiServlet(IServerInstance si) {
        super("http://localhost:3000");

    }
}