/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

import de.morihofi.certgine.types.intf.IServerInstance;
import jakarta.servlet.http.HttpServlet;
import lombok.Getter;
import lombok.NonNull;

import javax.naming.OperationNotSupportedException;
import java.util.Map;
import java.util.Set;

/**
 * Base contract for Certgine modules.
 *
 * <p>A module may contribute JPA entity classes, HTTP handlers and service
 * interfaces. Implementations are expected to provide the classes through the
 * corresponding getter methods.</p>
 */
public abstract class CertgineModule {

    /**
     * Reference to the active server instance. This may initially be {@code null}
     * when modules are loaded before the server has been fully constructed and
     * is populated later once the server instance becomes available.
     */
    @Getter
    private IServerInstance serverInstance;

    /**
     * Constructs a module with an optional server instance reference.
     *
     * @param serverInstance current server instance or {@code null} if not yet available
     */
    protected CertgineModule(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Updates the server instance reference once the server has been
     * constructed. Called by the core during startup to provide modules with the
     * fully initialised {@link IServerInstance}.
     *
     * @param serverInstance active server instance
     */
    public void setServerInstance(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
    }

    /**
     * Entity classes contributed by this module.
     *
     * @return immutable set of entity classes
     */
    public abstract Set<Class<?>> getEntityClasses();

    /**
     * HTTP handler classes contributed by this module.
     *
     * @return immutable set of HTTP handler classes
     */
    public abstract Set<Class<? extends HttpServlet>> getHttpServlets();

    /**
     * Runs on module gets registered
     */
    public void onRegister() {}

    /**
     * Runs on module gets unloaded
     */
    public void onUnLoad() {}

    /**
     * Runs as soon as serverinstance has been created
     * @param serverInstance server instance object
     */
    public void onModuleInitialize(IServerInstance serverInstance){}

    /**
     * Scheduled tasks contributed by this module.
     *
     * <p>The returned map must contain cron expressions as keys and
     * corresponding {@link ModuleScheduledTask} instances. Each task will be
     * scheduled by the core runtime according to the provided cron expression.</p>
     *
     * @return immutable map of cron expression to scheduled task
     */
    public Map<String, ModuleScheduledTask> getScheduledTasks() {
        return Map.of();
    }

    /**
     * Get an instance for interfacing with the current module
     * @return the module interface
     */
    @NonNull
    public CertgineModuleInstance getModuleInstance() {
        throw new IllegalArgumentException("This functionality is not implemented in your module");
    }
}

