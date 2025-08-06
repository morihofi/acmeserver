/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.ui.frontend.legacy;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;

/**
 * Factory for {@link LegacyWebUiModule}.
 */
public class LegacyWebUiModuleFactory implements CertgineModuleFactory {

    /**
     * Default constructor.
     */
    public LegacyWebUiModuleFactory() {
    }

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new LegacyWebUiModule(serverInstance);
    }
}

