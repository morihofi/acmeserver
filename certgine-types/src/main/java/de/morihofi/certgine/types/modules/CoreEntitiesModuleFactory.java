/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

import de.morihofi.certgine.types.intf.IServerInstance;

/**
 * Factory for {@link CoreEntitiesModule}.
 */
public class CoreEntitiesModuleFactory implements CertgineModuleFactory {

    /**
     * Default constructor.
     */
    public CoreEntitiesModuleFactory() {
    }

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new CoreEntitiesModule(serverInstance);
    }
}

