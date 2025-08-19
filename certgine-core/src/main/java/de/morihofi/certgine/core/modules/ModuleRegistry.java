/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.events.ModuleEntityChangeEvent;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServlet;
import java.util.*;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry containing all loaded modules and the classes they expose.
 */
@Data
@Slf4j
public class ModuleRegistry implements IModuleRegistry {

    @NonNull
    private final EventBus eventBus;

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
     * Scheduler managing all module-provided timed tasks.
     */
    private final ModuleTaskScheduler taskScheduler = new ModuleTaskScheduler();

    /**
     * Registers a module and adds its provided classes to the registry.
     *
     * <p>Only entity classes annotated with {@link jakarta.persistence.Entity}
     * are registered. HTTP servlet must extend {@link HttpServlet} and be
     * annotated with {@link ServletMount}.
     *
     * @param info module metadata and instance
     * @return {@code true} if the module was registered
     * @throws MissingDependencyException if the module is already registered or a dependency is missing
     */
    public boolean registerModule(@NonNull ModuleInfo info) {
        validateDependencies(info);

        String moduleName = info.getModuleName();
        CertgineModule module = info.getModule();

        // Reset tracked classes to reflect this registration cycle
        info.getEntityClasses().clear();
        info.getHttpHandlerClasses().clear();

        // Register entity classes with @Entity annotation
        for (Class<?> entityClass : module.getEntityClasses()) {
            if (entityClass.getAnnotation(Entity.class) == null) {
                log.warn("Skipping entity {} from module {}: missing @Entity annotation", entityClass.getName(), info.getModuleName());
                continue;
            }
            entityClasses.add(entityClass);
            info.getEntityClasses().add(entityClass);
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
            info.getHttpHandlerClasses().add(servletClass);
        }

        // Register services contributed by the module
        services.putAll(info.getServices());

        // Register scheduled tasks
        taskScheduler.registerModuleTasks(moduleName, module.getScheduledTasks());

        modules.put(moduleName, info);
        module.onRegister();

        eventBus.publish(new ModuleEntityChangeEvent(Set.copyOf(entityClasses)));
        return true;
    }

    /**
     * Ensures the module is not already registered and all required dependencies are present.
     *
     * @param info module metadata and instance
     * @throws MissingDependencyException if the module is already registered or a dependency is missing
     */
    private void validateDependencies(@NonNull ModuleInfo info) {
        String moduleName = info.getModuleName();
        if (modules.containsKey(moduleName)) {
            String message = String.format("Module %s is already registered", moduleName);
            log.error(message);
            throw new MissingDependencyException(message);
        }
        for (String dependency : info.getDependencies()) {
            if (!modules.containsKey(dependency)) {
                String message = String.format(
                        "Cannot register module %s: missing dependency %s", moduleName, dependency);
                log.error(message);
                throw new MissingDependencyException(message);
            }
        }
    }

    /**
     * Unregisters a previously loaded module and removes its provided classes
     * from the registry.
     *
     * @param moduleName unique name of the module to unload
     */
    public void unregisterModule(@NonNull String moduleName) {
        ModuleInfo info = modules.remove(moduleName);
        if (info == null) {
            return;
        }

        // Cancel scheduled tasks
        taskScheduler.cancelModuleTasks(moduleName);

        // Remove entity classes contributed by the module
        for (Class<?> entityClass : info.getEntityClasses()) {
            entityClasses.remove(entityClass);
        }

        // Remove HTTP servlets contributed by the module
        for (Class<? extends HttpServlet> servletClass : info.getHttpHandlerClasses()) {
            httpHandlerClasses.remove(servletClass);
        }

        // Remove services contributed by the module
        for (Class<?> serviceInterface : info.getServices().keySet()) {
            services.remove(serviceInterface);
        }

        info.getModule().onUnLoad();

        eventBus.publish(new ModuleEntityChangeEvent(Set.copyOf(entityClasses)));
    }

    /**
     * Reloads a module by unloading and registering it again.
     *
     * @param moduleName unique name of the module to reload
     */
    public void reloadModule(@NonNull String moduleName) {
        ModuleInfo info = modules.get(moduleName);
        if (info == null) {
            return;
        }
        unregisterModule(moduleName);
        registerModule(info);
    }

    /**
     * Registers a service implementation for a module after it has been loaded.
     *
     * <p>The service is stored both in the owning module's {@link ModuleInfo}
     * and in the global service map so that it participates in module unload
     * and reload operations.</p>
     *
     * @param moduleName name of the module that provides the service
     * @param iface      service interface class
     * @param impl       service implementation instance
     * @param <T>        type of the service
     */
    public <T> void registerService(
            @NonNull String moduleName,
            @NonNull Class<T> iface,
            @NonNull T impl) {
        ModuleInfo info = modules.get(moduleName);
        if (info == null) {
            log.error("Cannot register service {} for unknown module {}", iface.getName(), moduleName);
            return;
        }
        info.getServices().put(iface, impl);
        services.put(iface, impl);
    }

    /**
     * Retrieves a service implementation by its interface type.
     *
     * @param serviceInterface the service interface class
     * @param <T>              interface type
     * @return optional containing implementation instance if registered
     */
    public <T> Optional<T> getService(@NonNull Class<T> serviceInterface) {
        Object impl = services.get(serviceInterface);
        return Optional.ofNullable(serviceInterface.cast(impl));
    }

    /**
     * Exposes all registered service interfaces.
     *
     * @return immutable set of service interface classes
     */
    public Set<Class<?>> getServiceInterfaces() {
        return Collections.unmodifiableSet(services.keySet());
    }

    /**
     * Shuts down all scheduled tasks managed by the registry.
     */
    public void shutdownScheduler() {
        taskScheduler.shutdown();
    }
}

