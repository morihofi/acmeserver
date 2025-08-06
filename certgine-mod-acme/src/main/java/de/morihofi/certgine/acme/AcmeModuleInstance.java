package de.morihofi.certgine.acme;

import de.morihofi.certgine.acme.security.INonceManager;
import de.morihofi.certgine.acme.security.NonceManager;
import de.morihofi.certgine.types.modules.CertgineModuleInstance;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class AcmeModuleInstance extends CertgineModuleInstance {

    public AcmeModuleInstance(AcmeModule module){
        super(module);
        this.nonceManager = new NonceManager(module.getServerInstance());
    }

    @NonNull
    private final INonceManager nonceManager;
}
