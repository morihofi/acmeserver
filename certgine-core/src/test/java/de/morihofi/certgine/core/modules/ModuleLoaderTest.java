/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ModuleLoader}.
 */
class ModuleLoaderTest {

    /**
     * Verifies that modules are discovered and their classes registered.
     */
    @Test
    @Disabled("Requires CoreModule no-arg constructor")
    void loadModules_collectsClasses() {
        ModuleLoader loader = new ModuleLoader();
        ModuleRegistry registry = loader.loadModules();

        assertTrue(registry.getModules().containsKey("dummy"));
        assertTrue(registry.getEntityClasses().contains(DummyModule.DummyEntity.class));
        assertTrue(registry.getHttpHandlerClasses().contains(DummyModule.DummyServlet.class));
    }
}

