package de.morihofi.certgine.ui.frontend.modern;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;

import java.util.Set;

/**
 * Certgine module providing the modern web UI.
 */
@ModuleDescriptor(moduleName = "web-ui", description = "Modern web user interface")
public class WebUiModule extends CertgineModule {

    public WebUiModule(IServerInstance serverInstance) {
        super(serverInstance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(WebUiServlet.class);
    }
}
