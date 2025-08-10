/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.events.EventBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ModuleLoader}.
 */
class ModuleLoaderTest {

    /**
     * Verifies that modules are discovered and their classes registered.
     */
    @Test
    void loadModules_collectsClasses() {
        ModuleLoader loader = new ModuleLoader(new EventBus());
        ModuleRegistry registry = loader.loadModules();

        assertTrue(registry.getModules().containsKey("dummy"));
        assertTrue(registry.getEntityClasses().contains(DummyModule.DummyEntity.class));
        assertTrue(registry.getHttpHandlerClasses().contains(DummyModule.DummyServlet.class));
    }
}

