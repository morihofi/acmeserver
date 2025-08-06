/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

import de.morihofi.certgine.types.intf.IServerInstance;
import jakarta.servlet.http.HttpServlet;

import java.util.Map;
import java.util.Set;

/**
 * Base contract for Certgine modules.
 *
 * <p>A module may contribute JPA entity classes, HTTP handlers and service
 * interfaces. Implementations are expected to provide the classes through the
 * corresponding getter methods.</p>
 */
public interface CertgineModule {

    /**
     * Entity classes contributed by this module.
     *
     * @return immutable set of entity classes
     */
    Set<Class<?>> getEntityClasses();

    /**
     * HTTP handler classes contributed by this module.
     *
     * @return immutable set of HTTP handler classes
     */
    Set<Class<? extends HttpServlet>> getHttpServlets();


    /**
     * Runs on module gets registered
     */
    default void onRegister() {}

    /**
     * Runs on module gets unloaded
     */
    default void onUnLoad() {}

    /**
     * Runs as soon as serverinstance has been created
     * @param serverInstance server instance object
     */
    default void onModuleInitialize(IServerInstance serverInstance){}

    /**
     * Scheduled tasks contributed by this module.
     *
     * <p>The returned map must contain cron expressions as keys and
     * corresponding {@link ModuleScheduledTask} instances. Each task will be
     * scheduled by the core runtime according to the provided cron expression.</p>
     *
     * @return immutable map of cron expression to scheduled task
     */
    default Map<String, ModuleScheduledTask> getScheduledTasks() {
        return Map.of();
    }
}

