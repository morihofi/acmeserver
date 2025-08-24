package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.server.common.intf.ServletMount;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Test module exposing a {@link CertgineModuleInstance} and a servlet that
 * requires it for construction.
 */
@ModuleDescriptor(moduleName = "inst", description = "Module with instance for tests")
public class ModuleWithInstance extends CertgineModule {

    private final TestModuleInstance instance;

    public ModuleWithInstance() {
        super(null);
        this.instance = new TestModuleInstance(this);
    }

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of();
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(TestServlet.class);
    }

    @NotNull
    @Override
    public CertgineModuleInstance getModuleInstance() {
        return instance;
    }

    /**
     * Simple module instance used for testing.
     */
    static class TestModuleInstance extends CertgineModuleInstance {
        TestModuleInstance(CertgineModule module) {
            super(module);
        }
    }

    /**
     * Servlet capturing the passed module instance for assertions.
     */
    @ServletMount(servletMountPoint = "/inst")
    public static class TestServlet extends HttpServlet {
        public static CertgineModuleInstance capturedInstance;

        public TestServlet(CertgineModuleInstance moduleInstance) {
            capturedInstance = moduleInstance;
        }
    }
}
