/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;

/**
 * Factory for {@link AcmeModule}.
 */
public class AcmeModuleFactory implements CertgineModuleFactory {

    /**
     * Default constructor.
     */
    public AcmeModuleFactory() {
    }

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new AcmeModule(serverInstance);
    }
}

