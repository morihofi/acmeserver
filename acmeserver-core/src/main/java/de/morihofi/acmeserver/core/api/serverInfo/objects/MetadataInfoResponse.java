/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.api.serverInfo.objects;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents the metadata information response structure. This class encapsulates details such as version, build time, Git commit
 * identifier, Java version, and operating system information.
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public class MetadataInfoResponse {

    /**
     * The version of the application.
     */
    private String version;

    /**
     * The build time of the application.
     */
    @SerializedName("buildtime")
    private String buildTime;

    /**
     * The Git commit identifier of the build.
     */
    @SerializedName("gitcommit")
    private String gitCommit;

    /**
     * The Java version used in the build.
     */
    @SerializedName("javaversion")
    private String javaVersion;

    /**
     * The operating system information on which the build is running.
     */
    @SerializedName("os")
    private String operatingSystem;

    /**
     * The JVM uptime in milliseconds.
     */
    @SerializedName("jvmUptime")
    private long jvmUptime;

    /**
     * The JVM start time in milliseconds.
     */
    @SerializedName("jvmStartTime")
    private long jvmStartTime;

    /**
     * The startup time in milliseconds.
     */
    @SerializedName("startupTime")
    private long startupTime;

    /**
     * The host information.
     */
    @SerializedName("host")
    private String host;

    /**
     * The HTTPS port on which the application is running.
     */
    @SerializedName("httpsPort")
    private int httpsPort;

    /**
     * The update response information.
     */
    @SerializedName("update")
    private UpdateResponse update;

    /**
     * Retrieves the startup time.
     *
     * @return The startup time in milliseconds.
     */
    public long getStartupTime() {
        return startupTime;
    }

    /**
     * Sets the startup time.
     *
     * @param startupTime The new startup time in milliseconds.
     */
    public void setStartupTime(long startupTime) {
        this.startupTime = startupTime;
    }

    /**
     * Retrieves the JVM uptime.
     *
     * @return The JVM uptime in milliseconds.
     */
    public long getJvmUptime() {
        return jvmUptime;
    }

    /**
     * Sets the JVM uptime.
     *
     * @param jvmUptime The new JVM uptime in milliseconds.
     */
    public void setJvmUptime(long jvmUptime) {
        this.jvmUptime = jvmUptime;
    }

    /**
     * Retrieves the JVM start time.
     *
     * @return The JVM start time in milliseconds.
     */
    public long getJvmStartTime() {
        return jvmStartTime;
    }

    /**
     * Sets the JVM start time.
     *
     * @param jvmStartTime The new JVM start time in milliseconds.
     */
    public void setJvmStartTime(long jvmStartTime) {
        this.jvmStartTime = jvmStartTime;
    }

    /**
     * Retrieves the host information.
     *
     * @return The host information as a {@code String}.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the host information.
     *
     * @param host The new host information as a {@code String}.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Retrieves the HTTPS port.
     *
     * @return The HTTPS port as an integer.
     */
    public int getHttpsPort() {
        return httpsPort;
    }

    /**
     * Sets the HTTPS port.
     *
     * @param httpsPort The new HTTPS port as an integer.
     */
    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    /**
     * Retrieves the update response information.
     *
     * @return The update response as an {@code UpdateResponse} object.
     */
    public UpdateResponse getUpdate() {
        return update;
    }

    /**
     * Sets the update response information.
     *
     * @param update The new update response as an {@code UpdateResponse} object.
     */
    public void setUpdate(UpdateResponse update) {
        this.update = update;
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
     * Retrieves the Git commit identifier of the build. This identifier represents the specific commit in the Git repository that the build
     * is based on.
     *
     * @return The Git commit identifier as a {@code String}.
     */
    public String getGitCommit() {
        return gitCommit;
    }

    /**
     * Sets the Git commit identifier of the build. This method allows specifying the Git commit identifier that the build should
     * represent.
     *
     * @param gitCommit The Git commit identifier as a {@code String}.
     */
    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    /**
     * Retrieves the Java version used in the build. This version indicates the Java runtime environment version that the application is
     * built upon.
     *
     * @return The Java version as a {@code String}.
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * Sets the Java version used in the build. This method allows specifying the Java version that the application should be associated
     * with.
     *
     * @param javaVersion The Java version as a {@code String}.
     */
    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    /**
     * Retrieves the operating system information on which the build is running. This information includes the name and version of the
     * operating system.
     *
     * @return The operating system information as a {@code String}.
     */
    public String getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * Sets the operating system information for the build. This method allows specifying the operating system on which the build is
     * intended to run.
     *
     * @param operatingSystem The operating system information as a {@code String}.
     */
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }
}
