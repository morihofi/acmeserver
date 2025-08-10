/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.morihofi.certgine.core.util.DummyServerInstance;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.morihofi.certgine.types.events.EventBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests for {@link ModuleManager}.
 */
class ModuleManagerTest {

    private static final Path MODULE_PATH;

    static {
        try {
            MODULE_PATH =
                    Paths.get(DummyModule.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RegisterExtension
    static ModuleLifecycleExtension extension = ModuleLifecycleExtension.forPath(MODULE_PATH);

    /**
     * Verifies that a module can be loaded and its artifacts registered.
     */
    @Test
    void loadModuleRegistersArtifacts() {
        ModuleRegistry registry = extension.getModuleManager().getModuleRegistry();
        assertTrue(registry.getModules().containsKey("dummy"));
        assertTrue(registry.getEntityClasses().contains(DummyModule.DummyEntity.class));
        assertTrue(registry.getHttpHandlerClasses().contains(DummyModule.DummyServlet.class));
    }

    /**
     * Ensures {@link ModuleManager#loadModulesFromClasspath} passes the provided server instance to
     * modules.
     */
    @Test
    void loadModulesFromClasspath_passesServerInstance() {
        ModuleManager manager = new ModuleManager(new EventBus());
        DummyServerInstance serverInstance = new DummyServerInstance();

        manager.loadModulesFromClasspath(serverInstance);

        ModuleRegistry registry = manager.getModuleRegistry();
        assertTrue(registry.getModules().containsKey("serverAware"));
        ServerAwareModule module =
                (ServerAwareModule) registry.getModules().get("serverAware").getModule();
        assertSame(serverInstance, module.getServerInstance());

        manager.unloadModule("serverAware");
    }
}

