package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.core.modules.MissingDependencyException;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.modules.CertgineModule;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ModuleRegistry} unregister and reload operations.
 */
class ModuleRegistryTest {

    @Test
    void unregisterAndReloadModuleRemovesAndRestoresArtifacts() {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        ReloadableModule module = new ReloadableModule();

        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("test")
                .module(module)
                .services(Map.of(SampleService.class, new SampleServiceImpl()))
                .build());

        assertEquals(1, registry.getEntityClasses().size());
        assertEquals(1, registry.getHttpHandlerClasses().size());
        assertTrue(registry.getService(SampleService.class).isPresent());

        // Reload the module to ensure unloading and loading works in one step
        registry.reloadModule("test");
        assertEquals(1, registry.getEntityClasses().size());
        assertEquals(1, registry.getHttpHandlerClasses().size());
        assertTrue(registry.getService(SampleService.class).isPresent());

        // Finally unregister the module and ensure artifacts are removed
        registry.unregisterModule("test");
        assertTrue(module.unloaded);
        assertTrue(registry.getEntityClasses().isEmpty());
        assertTrue(registry.getHttpHandlerClasses().isEmpty());
        assertTrue(registry.getService(SampleService.class).isEmpty());
    }

    @Test
    void duplicateModulesAreRejected() {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        FlagModule first = new FlagModule();
        FlagModule second = new FlagModule();

        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("dup")
                .module(first)
                .build());
        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("dup")
                .module(second)
                .build());

        assertTrue(first.registered);
        assertFalse(second.registered);
        assertEquals(1, registry.getModules().size());
    }

    @Test
    void modulesRespectDependencyOrder() {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        FlagModule moduleA = new FlagModule();
        FlagModule moduleB = new FlagModule();

        ModuleRegistry.ModuleInfo infoB = ModuleRegistry.ModuleInfo.builder()
                .moduleName("B")
                .module(moduleB)
                .dependencies(Set.of("A"))
                .build();

        // Attempt to register B before A should be rejected
        registry.registerModule(infoB);
        assertEquals(0, registry.getModules().size());
        assertFalse(moduleB.registered);

        // Register dependency A first
        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("A")
                .module(moduleA)
                .build());
        assertEquals(1, registry.getModules().size());
        assertTrue(moduleA.registered);

        // Register B again now that dependency is satisfied
        registry.registerModule(infoB);
        assertEquals(2, registry.getModules().size());
        assertTrue(moduleB.registered);
    }

    @Test
    void validateDependenciesThrowsForMissing() throws Exception {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        ModuleRegistry.ModuleInfo info = ModuleRegistry.ModuleInfo.builder()
                .moduleName("B")
                .module(new FlagModule())
                .dependencies(Set.of("A"))
                .build();
        Method method = ModuleRegistry.class.getDeclaredMethod("validateDependencies", ModuleRegistry.ModuleInfo.class);
        method.setAccessible(true);
        assertThrows(MissingDependencyException.class, () -> {
            try {
                method.invoke(registry, info);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void validateDependenciesPassesWhenSatisfied() throws Exception {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("A")
                .module(new FlagModule())
                .build());
        ModuleRegistry.ModuleInfo info = ModuleRegistry.ModuleInfo.builder()
                .moduleName("B")
                .module(new FlagModule())
                .dependencies(Set.of("A"))
                .build();
        Method method = ModuleRegistry.class.getDeclaredMethod("validateDependencies", ModuleRegistry.ModuleInfo.class);
        method.setAccessible(true);
        assertDoesNotThrow(() -> {
            try {
                method.invoke(registry, info);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        });
    }

    @Test
    void dynamicServicesSurviveReloadAndUnregister() {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        FlagModule module = new FlagModule();

        ModuleRegistry.ModuleInfo info = ModuleRegistry.ModuleInfo.builder()
                .moduleName("dyn")
                .module(module)
                .build();

        registry.registerModule(info);
        SampleServiceImpl impl = new SampleServiceImpl();
        registry.registerService("dyn", SampleService.class, impl);

        Optional<SampleService> service = registry.getService(SampleService.class);
        assertTrue(service.isPresent());
        assertSame(impl, service.get());

        registry.unregisterModule("dyn");
        assertTrue(registry.getService(SampleService.class).isEmpty());

        registry.registerModule(info);
        Optional<SampleService> reloaded = registry.getService(SampleService.class);
        assertTrue(reloaded.isPresent());
        assertSame(impl, reloaded.get());
    }

    @Test
    void moduleInstanceCanBeAssignedLater() {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        ModuleWithInstance module = new ModuleWithInstance();
        ModuleRegistry.ModuleInfo info = ModuleRegistry.ModuleInfo.builder()
                .moduleName("inst")
                .module(module)
                .build();

        registry.registerModule(info);

        assertNull(registry.getModules().get("inst").getModuleInstance());

        info.setModuleInstance(module.getModuleInstance());
        assertSame(module.getModuleInstance(),
                registry.getModules().get("inst").getModuleInstance());
    }

    interface SampleService {
    }

    static class SampleServiceImpl implements SampleService {
    }

    static class ReloadableModule extends CertgineModule {
        boolean unloaded = false;

        ReloadableModule() {
            super(null);
        }

        @Override
        public Set<Class<?>> getEntityClasses() {
            return Set.of(DummyModule.DummyEntity.class);
        }

        @Override
        public Set<Class<? extends HttpServlet>> getHttpServlets() {
            return Set.of(DummyModule.DummyServlet.class);
        }

        @Override
        public void onUnLoad() {
            unloaded = true;
        }
    }

    static class FlagModule extends CertgineModule {
        boolean registered = false;

        FlagModule() {
            super(null);
        }

        @Override
        public Set<Class<?>> getEntityClasses() {
            return Set.of();
        }

        @Override
        public Set<Class<? extends HttpServlet>> getHttpServlets() {
            return Set.of();
        }

        @Override
        public void onRegister() {
            registered = true;
        }
    }
}
