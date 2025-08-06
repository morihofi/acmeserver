/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;

/**
 * Factory for {@link RevocationModule}.
 */
public class RevocationModuleFactory implements CertgineModuleFactory {

    /**
     * Default constructor.
     */
    public RevocationModuleFactory() {
    }

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new RevocationModule(serverInstance);
    }
}

