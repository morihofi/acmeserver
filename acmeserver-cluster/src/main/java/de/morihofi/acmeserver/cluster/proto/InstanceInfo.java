package de.morihofi.acmeserver.cluster.proto;

public class InstanceInfo {
    private String instanceId;

    public InstanceInfo() {
    }

    public InstanceInfo(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
