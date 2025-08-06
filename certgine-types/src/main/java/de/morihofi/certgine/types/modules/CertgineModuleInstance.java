package de.morihofi.certgine.types.modules;

import lombok.Getter;
import lombok.NonNull;

@Getter

public abstract class CertgineModuleInstance {

    @NonNull
    private final CertgineModule module;

    public CertgineModuleInstance(@NonNull CertgineModule module) {
        this.module = module;
    }
}
