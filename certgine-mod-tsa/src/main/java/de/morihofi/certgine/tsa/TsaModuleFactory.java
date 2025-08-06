/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.tsa;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;

/**
 * Factory for {@link TsaModule}.
 */
public class TsaModuleFactory implements CertgineModuleFactory {

    /**
     * Default constructor.
     */
    public TsaModuleFactory() {
    }

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new TsaModule(serverInstance);
    }
}

