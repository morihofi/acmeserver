package de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.ServerInfo.objects;

import com.google.gson.annotations.SerializedName;

public class MetadataInfoResponse {
    private String version;
    @SerializedName("buildtime")
    private String buildTime;
    @SerializedName("gitcommit")
    private String gitCommit;
    @SerializedName("javaversion")
    private String javaVersion;

    @SerializedName("os")
    private String operatingSystem;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }
}

