/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a {@link CertgineModule} by exposing metadata and provided classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
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

