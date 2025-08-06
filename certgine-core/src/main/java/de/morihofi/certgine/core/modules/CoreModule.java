package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.core.servlet.api.ApiServlet;
import de.morihofi.certgine.core.servlet.download.RootCaDownloadServlet;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;

import java.util.Set;

/**
 * Module exposing core servlet handlers bundled with the server.
 */
@ModuleDescriptor(moduleName = "core", description = "Core server handlers")
public class CoreModule implements CertgineModule {

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of();
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(
                RootCaDownloadServlet.class,
                ApiServlet.class
        );
    }
}
