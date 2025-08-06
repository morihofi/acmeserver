package de.morihofi.certgine.tsa;

import de.morihofi.certgine.tsa.servlets.TimeStampServlet;
import de.morihofi.certgine.tsa.types.entities.TsaAuthority;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;

import java.util.Set;

/**
 * Certgine module providing RFC 3161 timestamping servlet.
 */
@ModuleDescriptor(moduleName = "tsa", description = "RFC 3161 Time Stamp Authority")
public class TsaModule implements CertgineModule {

    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of(TsaAuthority.class);
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(TimeStampServlet.class);
    }
}
