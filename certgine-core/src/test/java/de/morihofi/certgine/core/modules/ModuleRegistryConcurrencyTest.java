package de.morihofi.certgine.core.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.morihofi.certgine.types.events.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/**
 * Tests concurrent registration and unregistration of modules.
 */
class ModuleRegistryConcurrencyTest {

    @Test
    void registerAndUnregisterModulesConcurrently() throws Exception {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        int count = 10;
        ExecutorService executor = Executors.newFixedThreadPool(count);

        List<Callable<Void>> registerTasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String name = "mod" + i;
            registerTasks.add(
                    () -> {
                        registry.registerModule(
                                ModuleRegistry.ModuleInfo.builder()
                                        .moduleName(name)
                                        .description("")
                                        .module(new DummyModule())
                                        .build());
                        return null;
                    });
        }
        for (var f : executor.invokeAll(registerTasks)) {
            f.get();
        }
        assertEquals(count, registry.getModules().size());

        List<Callable<Void>> unregisterTasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String name = "mod" + i;
            unregisterTasks.add(
                    () -> {
                        registry.unregisterModule(name);
                        return null;
                    });
        }
        for (var f : executor.invokeAll(unregisterTasks)) {
            f.get();
        }
        executor.shutdown();
        assertTrue(registry.getModules().isEmpty());
    }
}
