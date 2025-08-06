/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;
import java.util.Set;

/**
 * Test module that captures the provided {@link IServerInstance}.
 */
@ModuleDescriptor(moduleName = "serverAware", description = "captures server instance")
public class ServerAwareModule extends CertgineModule {

    public ServerAwareModule(IServerInstance serverInstance) {
        super(serverInstance);
    }

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of();
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of();
    }
}

