package de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the metadata information response structure.
 * This class encapsulates details such as version, build time, Git commit identifier,
 * Java version, and operating system information.
 */
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

    @SerializedName("jvmUptime")
    private long jvmUptime;

    @SerializedName("jvmStartTime")
    private long jvmStartTime;

    @SerializedName("startupTime")
    private long startupTime;

    @SerializedName("host")
    private String host;

    @SerializedName("httpsPort")
    private int httpsPort;

    public void setJvmUptime(long jvmUptime) {
        this.jvmUptime = jvmUptime;
    }

    public long getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(long startupTime) {
        this.startupTime = startupTime;
    }

    public long getJvmUptime() {
        return jvmUptime;
    }

    public long getJvmStartTime() {
        return jvmStartTime;
    }

    public void setJvmStartTime(long jvmStartTime) {
        this.jvmStartTime = jvmStartTime;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    /**
     * Retrieves the version information.
     *
     * @return The version as a {@code String}.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version information.
     *
     * @param version The new version as a {@code String}.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Retrieves the build time.
     *
     * @return The build time as a {@code String}.
     */
    public String getBuildTime() {
        return buildTime;
    }

    /**
     * Sets the build time.
     *
     * @param buildTime The new build time as a {@code String}.
     */
    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    /**
     * Retrieves the Git commit identifier of the build.
     * This identifier represents the specific commit in the Git repository that the build is based on.
     *
     * @return The Git commit identifier as a {@code String}.
     */
    public String getGitCommit() {
        return gitCommit;
    }

    /**
     * Sets the Git commit identifier of the build.
     * This method allows specifying the Git commit identifier that the build should represent.
     *
     * @param gitCommit The Git commit identifier as a {@code String}.
     */
    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    /**
     * Retrieves the Java version used in the build.
     * This version indicates the Java runtime environment version that the application is built upon.
     *
     * @return The Java version as a {@code String}.
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * Sets the Java version used in the build.
     * This method allows specifying the Java version that the application should be associated with.
     *
     * @param javaVersion The Java version as a {@code String}.
     */
    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    /**
     * Retrieves the operating system information on which the build is running.
     * This information includes the name and version of the operating system.
     *
     * @return The operating system information as a {@code String}.
     */
    public String getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * Sets the operating system information for the build.
     * This method allows specifying the operating system on which the build is intended to run.
     *
     * @param operatingSystem The operating system information as a {@code String}.
     */
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

}

