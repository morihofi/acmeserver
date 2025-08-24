package de.morihofi.certgine.clientinstallagent;

import de.morihofi.certgine.clientinstallagent.servlets.ClientInstallAgentServlet;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import de.morihofi.certgine.types.modules.ModuleDescriptor;
import jakarta.servlet.http.HttpServlet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@ModuleDescriptor(moduleName = "client-install-agent", description = "[Windows only] Provides an native tool that can be downloaded on a client you want to install one or multiple root certificates. It installs into Windows Root CA Store and patches all Java keystores")
public class ClientInstallAgentModule extends CertgineModule {

    /**
     * Constructs a module with an optional server instance reference.
     *
     * @param serverInstance current server instance or {@code null} if not yet available
     */
    protected ClientInstallAgentModule(IServerInstance serverInstance) {
        super(serverInstance);
    }

    @Override
    public Set<Class<? extends HttpServlet>> getHttpServlets() {
        return Set.of(ClientInstallAgentServlet.class);
    }

    @Override
    public @NonNull CertgineModuleInstance getModuleInstance() {
        return new ClientInstallAgentModuleInstance(this);
    }
}
