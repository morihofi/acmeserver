package de.morihofi.acmeserver.cluster.proto;

public class EventMessage {
    private String type;
    private String json;

    public EventMessage() {
    }

    public EventMessage(String type, String json) {
        this.type = type;
        this.json = json;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
