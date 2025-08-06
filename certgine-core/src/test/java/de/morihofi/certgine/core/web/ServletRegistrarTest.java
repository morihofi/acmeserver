package de.morihofi.certgine.core.web;

import de.morihofi.certgine.core.modules.DummyModule;
import de.morihofi.certgine.core.modules.ModuleRegistry;
import de.morihofi.certgine.core.modules.ModuleWithInstance;
import de.morihofi.certgine.types.intf.IServerInstance;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ServletRegistrar}.
 */
class ServletRegistrarTest {

    @Test
    void addBundledServlets_registersModuleServlets() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("dummy")
                .module(new DummyModule())
                .build());

        ServletRegistrar registrar = new ServletRegistrar(Mockito.mock(IServerInstance.class), registry);
        ServletContextHandler context = new ServletContextHandler();
        registrar.addBundledServlets(context);

        assertEquals(1, context.getServletHandler().getServlets().length);
        assertEquals(DummyModule.DummyServlet.class,
                context.getServletHandler().getServlets()[0].getHeldClass());
    }

    @Test
    void servletConstructorReceivesModuleInstance() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        ModuleWithInstance module = new ModuleWithInstance();
        registry.registerModule(ModuleRegistry.ModuleInfo.builder()
                .moduleName("inst")
                .module(module)
                .build());

        ServletRegistrar registrar = new ServletRegistrar(Mockito.mock(IServerInstance.class), registry);
        ServletContextHandler context = new ServletContextHandler();
        registrar.addBundledServlets(context);

        assertSame(module.getModuleInstance(), ModuleWithInstance.TestServlet.capturedInstance);
    }
}
