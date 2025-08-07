/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.core.util.DummyServerInstance;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ModuleServiceLoader}.
 */
class ModuleServiceLoaderTest {

    @Test
    void loadModules_passesServerInstance() {
        DummyServerInstance serverInstance = new DummyServerInstance();
        ModuleRegistry registry = ModuleServiceLoader.loadModules(Optional.of(serverInstance));

        assertTrue(registry.getModules().containsKey("serverAware"));
        ServerAwareModule module =
                (ServerAwareModule) registry.getModules().get("serverAware").getModule();
        assertSame(serverInstance, module.getServerInstance());
    }
}

