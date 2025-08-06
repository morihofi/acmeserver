/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.IModuleRegistry;
import de.morihofi.certgine.types.modules.ModuleScheduledTask;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;
import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServlet;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    /** Scheduler used for module-provided timed tasks. */
    private final TimedScheduler timedScheduler = new TimedScheduler();

    /** Handles of scheduled tasks keyed by module name. */
    private final Map<String, List<TimedScheduler.ScheduledHandle>> scheduledHandles = new HashMap<>();

    /** Original task instances keyed by module name. */
    private final Map<String, List<ModuleScheduledTask>> moduleTasks = new HashMap<>();

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
        String moduleName = info.getModuleName();
        if (modules.containsKey(moduleName)) {
            log.error("Module {} is already registered", moduleName);
            return;
        }
        for (String dependency : info.getDependencies()) {
            if (!modules.containsKey(dependency)) {
                log.error("Cannot register module {}: missing dependency {}", moduleName, dependency);
                return;
            }
        }

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
        Map<String, ModuleScheduledTask> scheduledTasks = module.getScheduledTasks();
        if (!scheduledTasks.isEmpty()) {
            List<TimedScheduler.ScheduledHandle> handles = new ArrayList<>();
            for (Map.Entry<String, ModuleScheduledTask> entry : scheduledTasks.entrySet()) {
                TimedScheduler.ScheduledHandle handle =
                        timedScheduler.schedule(entry.getKey(), entry.getValue().task());
                handles.add(handle);
            }
            scheduledHandles.put(moduleName, handles);
            moduleTasks.put(moduleName, new ArrayList<>(scheduledTasks.values()));
        }

        modules.put(moduleName, info);
        module.onRegister();
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
        List<TimedScheduler.ScheduledHandle> handles = scheduledHandles.remove(moduleName);
        if (handles != null) {
            handles.forEach(TimedScheduler.ScheduledHandle::cancel);
        }
        List<ModuleScheduledTask> tasks = moduleTasks.remove(moduleName);
        if (tasks != null) {
            tasks.forEach(ModuleScheduledTask::cancel);
        }

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

    /**
     * Shuts down all scheduled tasks and clears the scheduler.
     */
    public void shutdownScheduler() {
        timedScheduler.shutdown();
        scheduledHandles.clear();
        moduleTasks.clear();
    }
}

