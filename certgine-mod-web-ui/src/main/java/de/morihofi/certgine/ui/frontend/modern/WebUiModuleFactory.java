/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.ui.frontend.modern;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;

/**
 * Factory for {@link WebUiModule}.
 */
public class WebUiModuleFactory implements CertgineModuleFactory {

    /**
     * Default constructor.
     */
    public WebUiModuleFactory() {
    }

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new WebUiModule(serverInstance);
    }
}

