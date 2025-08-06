package de.morihofi.certgine.types.modules;

import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter

public abstract class CertgineModuleInstance {

    public CertgineModuleInstance(@NonNull CertgineModule module) {
        this.module = module;
    }

    @NonNull
    private final CertgineModule module;
}
