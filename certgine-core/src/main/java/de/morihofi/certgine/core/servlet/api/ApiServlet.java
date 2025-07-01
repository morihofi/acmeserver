/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.servlet.api;

import de.morihofi.certgine.server.common.intf.Endpoint;
import de.morihofi.certgine.server.common.intf.RoutableHttpServlet;
import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.httpserver.HandlerType;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.core.service.UserService;
import de.morihofi.certgine.core.servlet.api.core.LoginHandler;
import de.morihofi.certgine.core.servlet.api.core.RegisterHandler;
import de.morihofi.certgine.core.servlet.api.core.LogoutHandler;
import de.morihofi.certgine.core.servlet.api.core.StatusHandler;

@ServletMount(servletMountPoint = "/api/*", protect = true)
public class ApiServlet extends RoutableHttpServlet {
    private final IServerInstance serverInstance;
    private final UserService userService;

    public ApiServlet(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        this.userService = new UserService(serverInstance);
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/api/core/users/register", new RegisterHandler(userService)));
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/api/core/users/login", new LoginHandler(userService)));
        getRouter().addHandler(new Endpoint(HandlerType.POST, "/api/core/users/logout", new LogoutHandler(serverInstance)));
        getRouter().addHandler(new Endpoint(HandlerType.GET, "/api/core/status", new StatusHandler(userService)));
    }
}
