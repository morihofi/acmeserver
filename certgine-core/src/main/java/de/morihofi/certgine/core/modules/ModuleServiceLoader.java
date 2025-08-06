/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import java.util.Optional;
import java.util.ServiceLoader;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for discovering {@link CertgineModule} implementations via {@link ServiceLoader}.
 *
 * <p>The returned {@link ModuleRegistry} contains all modules found on the classpath.</p>
 */
@Slf4j
public final class ModuleServiceLoader {

    private ModuleServiceLoader() {
    }

    /**
     * Loads all {@link CertgineModuleFactory} implementations from the classpath.
     *
     * @param serverInstance optional server instance passed to module factories
     * @return registry populated with discovered modules
     */
    public static ModuleRegistry loadModules(@NonNull Optional<IServerInstance> serverInstance) {
        ModuleRegistry registry = new ModuleRegistry();
        ServiceLoader<CertgineModuleFactory> serviceLoader =
                ServiceLoader.load(CertgineModuleFactory.class);
        for (CertgineModuleFactory factory : serviceLoader) {
            CertgineModule module = factory.create(serverInstance.orElse(null));
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
            registry.registerModule(info);
            log.info("Loaded module {}", descriptor.moduleName());
        }
        return registry;
    }
}

