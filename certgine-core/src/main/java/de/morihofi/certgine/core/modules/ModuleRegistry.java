/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.modules.CertgineModule;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry containing all loaded modules and the classes they expose.
 */
@Data
public class ModuleRegistry {

    /**
     * Loaded modules keyed by their unique name.
     */
    private final Map<String, ModuleInfo> modules = new HashMap<>();

    /**
     * Collected JPA entity classes from all modules.
     */
    private final Set<Class<?>> entityClasses = new HashSet<>();

    /**
     * Collected HTTP handler classes from all modules.
     */
    private final Set<Class<?>> httpHandlerClasses = new HashSet<>();

    /**
     * Collected service interface classes from all modules.
     */
    private final Set<Class<?>> serviceInterfaces = new HashSet<>();

    /**
     * Registers a module and adds its provided classes to the registry.
     *
     * @param info module metadata and instance
     */
    public void registerModule(@NonNull ModuleInfo info) {
        modules.put(info.getModuleName(), info);
        entityClasses.addAll(info.getModule().getEntityClasses());
        httpHandlerClasses.addAll(info.getModule().getHttpHandlerClasses());
        serviceInterfaces.addAll(info.getModule().getServiceInterfaces());
    }

    /**
     * Metadata about a loaded module.
     */
    @Data
    @Builder
    public static class ModuleInfo {

        /**
         * Unique name of the module.
         */
        @NonNull
        private final String moduleName;

        /**
         * Short description of the module.
         */
        private final String description;

        /**
         * The module implementation instance.
         */
        @NonNull
        private final CertgineModule module;
    }
}

