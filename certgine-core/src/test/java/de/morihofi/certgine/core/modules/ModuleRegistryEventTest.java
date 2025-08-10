/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.events.ModuleEntityChangeEvent;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServlet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link ModuleRegistry} emits {@link ModuleEntityChangeEvent} on changes.
 */
class ModuleRegistryEventTest {

    @Test
    void registerUnregisterEmitsEvents() {
        EventBus bus = new EventBus();
        ModuleRegistry registry = new ModuleRegistry(bus);
        List<ModuleEntityChangeEvent> events = new ArrayList<>();
        bus.subscribe(ModuleEntityChangeEvent.class, events::add);

        ModuleRegistry.ModuleInfo info = ModuleRegistry.ModuleInfo.builder()
                .moduleName("test")
                .module(new SimpleModule())
                .build();

        registry.registerModule(info);
        assertEquals(1, events.size());
        assertTrue(events.get(0).getEntityClasses().contains(SimpleEntity.class));

        registry.unregisterModule("test");
        assertEquals(2, events.size());
        assertFalse(events.get(1).getEntityClasses().contains(SimpleEntity.class));
    }

    @ModuleDescriptor(moduleName = "test", description = "")
    static class SimpleModule extends CertgineModule {
        SimpleModule() {
            super(null);
        }

        @Override
        public Set<Class<?>> getEntityClasses() {
            return Set.of(SimpleEntity.class);
        }

        @Override
        public Set<Class<? extends HttpServlet>> getHttpServlets() {
            return Set.of();
        }
    }

    @Entity
    static class SimpleEntity {
    }
}

