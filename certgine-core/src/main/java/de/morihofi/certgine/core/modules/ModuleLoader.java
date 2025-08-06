/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import java.util.Optional;

/**
 * Loads modules using {@link ModuleServiceLoader}.
 */
public class ModuleLoader {

    /**
     * Discovers modules via {@link java.util.ServiceLoader} and returns a populated registry.
     *
     * @return registry containing all discovered modules
     */
    public ModuleRegistry loadModules() {
        return ModuleServiceLoader.loadModules(Optional.empty());
    }
}

