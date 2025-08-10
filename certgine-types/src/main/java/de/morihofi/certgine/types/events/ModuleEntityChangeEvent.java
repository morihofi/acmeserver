/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fired whenever the set of entity classes provided by loaded modules changes.
 */
@AllArgsConstructor
@Getter
public class ModuleEntityChangeEvent extends AbstractEvent {
    private final Set<Class<?>> entityClasses;
}

