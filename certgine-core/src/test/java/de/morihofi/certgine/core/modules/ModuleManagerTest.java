/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ModuleManager}.
 */
class ModuleManagerTest {

    /**
     * Verifies that a module can be loaded and unloaded correctly.
     */
    @Test
    void loadAndUnloadModule() throws Exception {
        ModuleManager manager = new ModuleManager();
        Path modulePath = Paths.get(DummyModule.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        manager.loadModule(modulePath);

        ModuleRegistry registry = manager.getModuleRegistry();
        assertTrue(registry.getModules().containsKey("dummy"));
        assertTrue(registry.getEntityClasses().contains(DummyModule.DummyEntity.class));
        assertTrue(registry.getHttpHandlerClasses().contains(DummyModule.DummyServlet.class));

        manager.unloadModule("dummy");
        assertFalse(registry.getModules().containsKey("dummy"));
        assertFalse(registry.getEntityClasses().contains(DummyModule.DummyEntity.class));
        assertFalse(registry.getHttpHandlerClasses().contains(DummyModule.DummyServlet.class));
    }
}

