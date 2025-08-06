/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 * Loads {@link CertgineModule} implementations using {@link ServiceLoader} and
 * registers them in a {@link ModuleRegistry}.
 */
@Slf4j
public class ModuleLoader {

    /**
     * Discovers modules via {@link ServiceLoader} and returns a populated registry.
     *
     * @return registry containing all discovered modules
     */
    public ModuleRegistry loadModules() {
        ModuleRegistry registry = new ModuleRegistry();
        ServiceLoader<CertgineModuleFactory> serviceLoader =
                ServiceLoader.load(CertgineModuleFactory.class);
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
            registry.registerModule(info);
            log.info("Loaded module {}", descriptor.moduleName());
        }
        return registry;
    }
}

