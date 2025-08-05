/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;

import java.util.Set;

/**
 * Simple module used for testing the {@link ModuleLoader}.
 */
@ModuleDescriptor(moduleName = "dummy", description = "Test module")
public class DummyModule implements CertgineModule {

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of(DummyEntity.class);
    }

    @Override
    public Set<Class<?>> getHttpHandlerClasses() {
        return Set.of(DummyHandler.class);
    }

    @Override
    public Set<Class<?>> getServiceInterfaces() {
        return Set.of(DummyService.class);
    }

    /** Dummy entity class. */
    public static class DummyEntity {}

    /** Dummy handler class. */
    public static class DummyHandler {}

    /** Dummy service interface. */
    public interface DummyService {}
}

