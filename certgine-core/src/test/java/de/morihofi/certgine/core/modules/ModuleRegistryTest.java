package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.modules.CertgineModule;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ModuleRegistry} unregister and reload operations.
 */
class ModuleRegistryTest {

    interface SampleService {}

    static class SampleServiceImpl implements SampleService {}

    static class ReloadableModule implements CertgineModule {
        boolean unloaded = false;

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

    @Test
    void unregisterAndReloadModuleRemovesAndRestoresArtifacts() {
        ModuleRegistry registry = new ModuleRegistry();
        ReloadableModule module = new ReloadableModule();

        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("test")
                .module(module)
                .services(Map.of(SampleService.class, new SampleServiceImpl()))
                .build());

        assertEquals(1, registry.getEntityClasses().size());
        assertEquals(1, registry.getHttpHandlerClasses().size());
        assertNotNull(registry.getService(SampleService.class));

        // Reload the module to ensure unloading and loading works in one step
        registry.reloadModule("test");
        assertEquals(1, registry.getEntityClasses().size());
        assertEquals(1, registry.getHttpHandlerClasses().size());
        assertNotNull(registry.getService(SampleService.class));

        // Finally unregister the module and ensure artifacts are removed
        registry.unregisterModule("test");
        assertTrue(module.unloaded);
        assertTrue(registry.getEntityClasses().isEmpty());
        assertTrue(registry.getHttpHandlerClasses().isEmpty());
        assertNull(registry.getService(SampleService.class));
    }
}
