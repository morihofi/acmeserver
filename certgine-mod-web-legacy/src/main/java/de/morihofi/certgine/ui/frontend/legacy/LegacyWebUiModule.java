package de.morihofi.certgine.ui.frontend.legacy;

import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;

import java.util.Set;

/**
 * Certgine module providing the legacy web UI.
 */
@ModuleDescriptor(moduleName = "web-legacy", description = "Legacy web user interface")
public class LegacyWebUiModule implements CertgineModule {

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
        return Set.of(LegacyWebUiServlet.class);
    }
}
