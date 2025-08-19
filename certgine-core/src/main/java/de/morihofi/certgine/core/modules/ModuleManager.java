/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manager responsible for loading and unloading modules.
 *
 * <p>Every module is loaded through its own {@link URLClassLoader}. The loaded
 * module is registered in a shared {@link ModuleRegistry} which exposes the
 * collected classes to the rest of the application.</p>
 *
 * <p>This class is thread-safe. Calls to {@link #loadModule(Path)} and
 * {@link #unloadModule(String)} are synchronized to allow concurrent
 * module management from multiple threads.</p>
 */
@Slf4j
public class ModuleManager {

    private final EventBus eventBus;

    /**
     * Registry containing metadata about the loaded modules.
     */
    @Getter
    private ModuleRegistry moduleRegistry;

    /**
     * Class loaders keyed by module name.
     */
    private final ConcurrentMap<String, URLClassLoader> loaders = new ConcurrentHashMap<>();

    public ModuleManager(@NonNull EventBus eventBus) {
        this.eventBus = eventBus;
        this.moduleRegistry = new ModuleRegistry(eventBus);
    }

    /**
     * Loads a module from the given JAR file or directory.
     *
     * @param jar location of the module JAR or directory
     * @throws IOException      if the module cannot be read
     * @throws RuntimeException if the module cannot be registered
     */
    public synchronized void loadModule(@NonNull Path jar) throws IOException {
        URL url = jar.toUri().toURL();
        URLClassLoader cl = new URLClassLoader(new URL[]{url}, getClass().getClassLoader());

        ServiceLoader<CertgineModuleFactory> serviceLoader =
                ServiceLoader.load(CertgineModuleFactory.class, cl);
        for (CertgineModuleFactory factory : serviceLoader) {
            CertgineModule module = factory.create(null);
            ModuleDescriptor descriptor = module.getClass().getAnnotation(ModuleDescriptor.class);
            if (descriptor == null) {
                log.warn("Ignoring module {} without @ModuleDescriptor", module.getClass().getName());
                continue;
            }

            ModuleRegistry.ModuleInfo info = ModuleRegistry.ModuleInfo.builder()
                    .moduleName(descriptor.moduleName())
                    .description(descriptor.description())
                    .module(module)
                    .build();

            try {
                if (moduleRegistry.registerModule(info)) {
                    loaders.put(descriptor.moduleName(), cl);
                    log.info("Loaded module {} from {}", descriptor.moduleName(), jar);
                }
            } catch (RuntimeException e) {
                try {
                    cl.close();
                } catch (IOException closeException) {
                    log.warn("Failed to close class loader for module {}", descriptor.moduleName(), closeException);
                }
                throw e;
            }
        }
    }

    /**
     * Discovers and loads modules that are already present on the application
     * classpath using {@link ServiceLoader}.
     */
    public void loadModulesFromClasspath(IServerInstance serverInstance) {
        moduleRegistry = ModuleServiceLoader.loadModules(Optional.ofNullable(serverInstance), eventBus);
    }

    /**
     * Unloads a previously loaded module.
     *
     * @param moduleName unique name of the module to unload
     */
    public synchronized void unloadModule(@NonNull String moduleName) {
        moduleRegistry.unregisterModule(moduleName);
        URLClassLoader cl = loaders.remove(moduleName);
        if (cl != null) {
            try {
                cl.close();
            } catch (IOException e) {
                log.warn("Failed to close class loader for module {}", moduleName, e);
            }
        }
    }
}

