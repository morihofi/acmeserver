/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.events.EventBus;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that loads a module before each test and unloads it afterwards.
 */
public class ModuleLifecycleExtension implements BeforeEachCallback, AfterEachCallback {

    private final Path modulePath;
    private ModuleManager moduleManager;

    private ModuleLifecycleExtension(Path modulePath) {
        this.modulePath = modulePath;
    }

    /**
     * Creates a new extension for the given module path.
     *
     * @param modulePath path to the module JAR or directory
     * @return configured {@code ModuleLifecycleExtension}
     */
    public static ModuleLifecycleExtension forPath(Path modulePath) {
        return new ModuleLifecycleExtension(modulePath);
    }

    /**
     * Returns the {@link ModuleManager} used by this extension.
     *
     * @return module manager instance
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * Loads the configured module before each test.
     *
     * @param context current extension context
     * @throws Exception if the module cannot be loaded
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        moduleManager = new ModuleManager(new EventBus());
        try {
            moduleManager.loadModule(modulePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load module", e);
        }
    }

    /**
     * Unloads the previously loaded module after each test.
     *
     * @param context current extension context
     */
    @Override
    public void afterEach(ExtensionContext context) {
        if (moduleManager != null) {
            for (String name : new HashSet<>(moduleManager.getModuleRegistry().getModules().keySet())) {
                try {
                    moduleManager.unloadModule(name);
                } catch (Exception ignored) {
                    // Ignore unload failures in tests
                }
            }
            moduleManager = null;
        }
    }
}

