/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.servlet.api;

import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.intf.IServerInstance;

/**
 * Servlet responsible for handling API requests mounted under {@code /api}.
 */
@ServletMount(servletMountPoint = "/api/*", protect = true)
public class ApiServlet extends RoutableHttpServlet {
    private final IServerInstance serverInstance;

    /**
     * Constructs a new {@code ApiServlet}.
     *
     * @param serverInstance the server instance used to access shared services
     */
    public ApiServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }
}
