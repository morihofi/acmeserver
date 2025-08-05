package de.morihofi.certgine.types.modules;

import de.morihofi.certgine.types.database.entities.authority.*;
import de.morihofi.certgine.types.database.entities.timestamp.TsaAuthority;
import de.morihofi.certgine.types.database.entities.user.UserSession;
import de.morihofi.certgine.types.database.entities.user.Users;
import jakarta.servlet.http.HttpServlet;

import java.util.Set;

/**
 * Module exposing core Certgine entities shared across features.
 */
@ModuleDescriptor(moduleName = "types-entities", description = "Core database entities")
public class CoreEntitiesModule implements CertgineModule {

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of(
                RootCa.class,
                IntermediateCa.class,
                CertificateAlgorithm.class,
                RsaCertificateAlgorithm.class,
                EcdsaCertificateAlgorithm.class,
                CertificateConfig.class,
                CertificateExpiration.class,
                CertificateMetadata.class,
                TsaAuthority.class,
                Users.class,
                UserSession.class
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of();
    }
}
