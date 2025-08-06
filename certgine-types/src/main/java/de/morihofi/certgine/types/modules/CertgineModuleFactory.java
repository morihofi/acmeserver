/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

import de.morihofi.certgine.types.intf.IServerInstance;

/**
 * Factory for creating {@link CertgineModule} instances.
 */
public interface CertgineModuleFactory {

    /**
     * Creates a new module instance bound to the given server instance.
     *
     * @param serverInstance the running server instance
     * @return newly created module
     */
    CertgineModule create(IServerInstance serverInstance);
}

