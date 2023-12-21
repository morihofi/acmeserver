package de.morihofi.acmeserver.certificate.acme.api.endpoints.objects;

public class Identifier {
    private String type;
    private String value;

    public Identifier(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Identifier() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
