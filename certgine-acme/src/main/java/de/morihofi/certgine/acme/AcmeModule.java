package de.morihofi.certgine.acme;

import de.morihofi.certgine.acme.servlets.AcmeHttpServlet;
import de.morihofi.certgine.acme.servlets.GetHttpsForFreeServlet;
import de.morihofi.certgine.acme.types.entities.*;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;

import java.util.Set;

/**
 * Certgine module providing ACME functionality.
 */
@ModuleDescriptor(moduleName = "acme", description = "ACME API and related entities")
public class AcmeModule implements CertgineModule {

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getEntityClasses() {
        return Set.of(
                AcmeAccount.class,
                AcmeExternalAccountBinding.class,
                AcmeOrder.class,
                AcmeOrderIdentifier.class,
                AcmeOrderIdentifierChallenge.class,
                AcmeProvisioner.class,
                AcmeProvisionerDomainNameRestriction.class,
                HttpNonces.class,
                ProvisionerMeta.class
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(
                AcmeHttpServlet.class,
                GetHttpsForFreeServlet.class
        );
    }
}
