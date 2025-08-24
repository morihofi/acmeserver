package de.morihofi.certgine.clientinstallagent;

import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleFactory;

public class ClientInstallAgentModuleFactory implements CertgineModuleFactory {

    /**
     * Default constructor.
     */
    public ClientInstallAgentModuleFactory() {
    }

    @Override
    public CertgineModule create(IServerInstance serverInstance) {
        return new ClientInstallAgentModule(serverInstance);
    }
}


