package de.morihofi.acmeserver.config.keyStoreHelpers;

import java.io.Serializable;

public abstract class KeyStoreParams implements Serializable {
    protected String type;
    protected String password;

    public String getPassword(){
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
