package de.morihofi.acmeserver.config.keyStoreHelpers;

public class PKCS12KeyStoreParams extends KeyStoreParams {

    public PKCS12KeyStoreParams(){
        type = "pkcs12";
    }
    private String location;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
