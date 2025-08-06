/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;

/**
 * Factory for {@link ServerAwareModule}.
 */
public class ServerAwareModuleFactory implements CertgineModuleFactory {

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new ServerAwareModule(serverInstance);
    }
}

