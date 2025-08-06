package de.morihofi.certgine.revocation;

import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;

import java.util.Set;

/**
 * Module exposing certificate revocation HTTP endpoints.
 */
@ModuleDescriptor(moduleName = "revocation", description = "Certificate revocation endpoints")
public class RevocationModule implements CertgineModule {

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of();
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(RevocationHttpServlet.class);
    }
}
