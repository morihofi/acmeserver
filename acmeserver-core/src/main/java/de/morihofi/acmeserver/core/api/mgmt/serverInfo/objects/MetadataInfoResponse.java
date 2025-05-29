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

package de.morihofi.acmeserver.core.api.mgmt.serverInfo.objects;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

/**
 * Represents the metadata information response structure. This class encapsulates details such as version, build time, Git commit
 * identifier, Java version, and operating system information.
 */
@Data
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

}
