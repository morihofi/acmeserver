package de.morihofi.certgine.clientinstallagent;

import de.morihofi.certgine.clientinstallagent.builder.AgentGenerator;
import de.morihofi.certgine.types.modules.CertgineModule;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import lombok.Getter;
import lombok.NonNull;

public class ClientInstallAgentModuleInstance extends CertgineModuleInstance {

    @NonNull
    @Getter
    private final AgentGenerator agentGenerator;

    public ClientInstallAgentModuleInstance(@NonNull ClientInstallAgentModule module) {
        super(module);
        agentGenerator = new AgentGenerator(this);
    }

}
