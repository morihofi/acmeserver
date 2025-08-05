/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServlet;

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
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(DummyServlet.class);
    }


    /** Dummy entity class. */
    @Entity
    public static class DummyEntity {}

    /** Dummy servlet class. */
    @ServletMount(servletMountPoint = "/dummy")
    public static class DummyServlet extends HttpServlet {}
}

