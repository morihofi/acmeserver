package de.morihofi.certgine.core.web;

import de.morihofi.certgine.core.modules.DummyModule;
import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.core.modules.ModuleWithInstance;
import de.morihofi.certgine.core.util.DummyServerInstance;
import de.morihofi.certgine.types.events.EventBus;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ServletRegistrar}.
 */
class ServletRegistrarTest {

    @Test
    void addBundledServlets_registersModuleServlets() throws Exception {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("dummy")
                .module(new DummyModule())
                .build());

        ServletRegistrar registrar = new ServletRegistrar(new DummyServerInstance(), registry);
        ServletContextHandler context = new ServletContextHandler();
        registrar.addBundledServlets(context);

        assertEquals(1, context.getServletHandler().getServlets().length);
        assertEquals(DummyModule.DummyServlet.class,
                context.getServletHandler().getServlets()[0].getHeldClass());
    }

    @Test
    void servletConstructorReceivesModuleInstance() throws Exception {
        ModuleRegistry registry = new ModuleRegistry(new EventBus());
        ModuleWithInstance module = new ModuleWithInstance();
        ModuleRegistry.ModuleInfo info = ModuleRegistry.ModuleInfo.builder()
                .moduleName("inst")
                .module(module)
                .build();
        registry.registerModule(info);

        DummyServerInstance serverInstance = new DummyServerInstance();
        module.setServerInstance(serverInstance);
        info.setModuleInstance(module.getModuleInstance());

        ServletRegistrar registrar = new ServletRegistrar(serverInstance, registry);
        ServletContextHandler context = new ServletContextHandler();
        registrar.addBundledServlets(context);

        assertSame(module.getModuleInstance(), ModuleWithInstance.TestServlet.capturedInstance);
    }
}
