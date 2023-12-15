package de.morihofi.acmeserver.config.keyStoreHelpers;

import java.io.Serializable;

public abstract class KeyStoreParams implements Serializable {

    private String password;

    public String getPassword(){
        return password;
    }
}
