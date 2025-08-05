/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

import de.morihofi.certgine.types.intf.IServerInstance;

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
    Set<Class<?>> getHttpHandlerClasses();

    /**
     * Service interface classes contributed by this module.
     *
     * @return immutable set of service interface classes
     */
    Set<Class<?>> getServiceInterfaces();

    /**
     * Runs on module gets loaded
     */
    default void onLoad() {}

    /**
     * Runs on module gets unloaded
     */
    default void onUnLoad() {}
}

