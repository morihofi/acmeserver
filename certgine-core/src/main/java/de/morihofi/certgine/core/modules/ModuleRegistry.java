/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServlet;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry containing all loaded modules and the classes they expose.
 */
@Data
@Slf4j
public class ModuleRegistry implements IModuleRegistry {

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
    private final Set<Class<? extends HttpServlet>> httpHandlerClasses = new HashSet<>();

    /**
     * Mapping of service interfaces to their implementation instances.
     */
    private final Map<Class<?>, Object> services = new HashMap<>();

    /**
     * Registers a module and adds its provided classes to the registry.
     *
     * <p>Only entity classes annotated with {@link jakarta.persistence.Entity}
     * are registered. HTTP servlet must extend {@link HttpServlet} and be
     * annotated with {@link ServletMount}.
     *
     * @param info module metadata and instance
     */
    public void registerModule(@NonNull ModuleInfo info) {
        CertgineModule module = info.getModule();

        // Register entity classes with @Entity annotation
        for (Class<?> entityClass : module.getEntityClasses()) {
            if (entityClass.getAnnotation(Entity.class) == null) {
                log.warn("Skipping entity {} from module {}: missing @Entity annotation", entityClass.getName(), info.getModuleName());
                continue;
            }
            entityClasses.add(entityClass);
        }

        // Register valid HTTP servlet classes
        for (Class<? extends HttpServlet> servletClass : module.getHttpServlets()) {
            if (!HttpServlet.class.isAssignableFrom(servletClass)) {
                log.warn("Skipping HTTP servlet {} from module {}: not a HttpServlet", servletClass.getName(), info.getModuleName());
                continue;
            }
            if (servletClass.getAnnotation(ServletMount.class) == null) {
                log.warn("Skipping HTTP servlet {} from module {}: missing @ServletMount annotation", servletClass.getName(), info.getModuleName());
                continue;
            }
            httpHandlerClasses.add(servletClass);
        }


        modules.put(info.getModuleName(), info);
        module.onRegister();
    }

    /**
     * Retrieves a service implementation by its interface type.
     *
     * @param serviceInterface the service interface class
     * @param <T>              interface type
     * @return implementation instance or {@code null} if none registered
     */
    public <T> T getService(@NonNull Class<T> serviceInterface) {
        Object impl = services.get(serviceInterface);
        if (impl == null) {
            return null;
        }
        return serviceInterface.cast(impl);
    }

    /**
     * Exposes all registered service interfaces.
     *
     * @return immutable set of service interface classes
     */
    public Set<Class<?>> getServiceInterfaces() {
        return Collections.unmodifiableSet(services.keySet());
    }
}

