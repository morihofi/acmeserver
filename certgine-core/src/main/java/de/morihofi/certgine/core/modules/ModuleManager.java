/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

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
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Manager responsible for loading and unloading modules.
 *
 * <p>Every module is loaded through its own {@link URLClassLoader}. The loaded
 * module is registered in a shared {@link ModuleRegistry} which exposes the
 * collected classes to the rest of the application.</p>
 */
@Slf4j
public class ModuleManager {

    /**
     * Registry containing metadata about the loaded modules.
     */
    @Getter
    private final ModuleRegistry moduleRegistry = new ModuleRegistry();

    /**
     * Class loaders keyed by module name.
     */
    private final Map<String, URLClassLoader> loaders = new HashMap<>();

    /**
     * Loads a module from the given JAR file or directory.
     *
     * @param jar location of the module JAR or directory
     * @throws IOException if the module cannot be read
     */
    public void loadModule(@NonNull Path jar) throws IOException {
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

            moduleRegistry.registerModule(info);
            loaders.put(descriptor.moduleName(), cl);
            log.info("Loaded module {} from {}", descriptor.moduleName(), jar);
        }
    }

    /**
     * Discovers and loads modules that are already present on the application
     * classpath using {@link ServiceLoader}.
     */
    public void loadModulesFromClasspath(IServerInstance serverInstance) {
        ServiceLoader<CertgineModuleFactory> serviceLoader =
                ServiceLoader.load(CertgineModuleFactory.class);
        for (CertgineModuleFactory factory : serviceLoader) {
            CertgineModule module = factory.create(serverInstance);
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
            moduleRegistry.registerModule(info);
            log.info("Loaded module {}", descriptor.moduleName());
        }
    }

    /**
     * Unloads a previously loaded module.
     *
     * @param moduleName unique name of the module to unload
     */
    public void unloadModule(@NonNull String moduleName) {
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

