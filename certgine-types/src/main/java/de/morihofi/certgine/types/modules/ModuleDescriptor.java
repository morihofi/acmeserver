/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

/**
 * Describes a {@link CertgineModule} by exposing metadata and provided classes.
 */
public @interface ModuleDescriptor {

    /**
     * Unique identifier of the module.
     *
     * @return module identifier
     */
    String moduleName();

    /**
     * Short human readable description of the module.
     *
     * @return module description
     */
    String description() default "";
}

