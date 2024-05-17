package de.morihofi.acmeserver.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.lang.String;

/**
 * Represents configuration parameters for a server, including DNS name and port settings.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class ServerConfig implements Serializable {
    private String dnsName = "example.com";
    private Ports ports = new Ports();
    private boolean enableSniCheck = true;
    private String loggingDirectory = null;

    private MozillaSslConfig mozillaSslConfig = new MozillaSslConfig();
    private SslServerConfig sslServerConfig = new SslServerConfig();

    /**
     * Get the DNS name of the server.
     *
     * @return The DNS name.
     */
    public String getDnsName() {
        return dnsName;
    }

    /**
     * Set the DNS name of the server.
     *
     * @param dnsName The DNS name to set.
     */
    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    /**
     * Get the port configuration for the server.
     *
     * @return The port configuration.
     */
    public Ports getPorts() {
        return this.ports;
    }

    /**
     * Set the port configuration for the server.
     *
     * @param ports The port configuration to set.
     */

    public void setPorts(Ports ports) {
        this.ports = ports;
    }

    public boolean isEnableSniCheck() {
        return enableSniCheck;
    }

    public String getLoggingDirectory() {
        return loggingDirectory;
    }

    public void setLoggingDirectory(String loggingDirectory) {
        this.loggingDirectory = loggingDirectory;
    }

    public void setEnableSniCheck(boolean enableSniCheck) {
        this.enableSniCheck = enableSniCheck;
    }

    public MozillaSslConfig getMozillaSslConfig() {
        return mozillaSslConfig;
    }

    public void setMozillaSslConfig(MozillaSslConfig mozillaSslConfig) {
        this.mozillaSslConfig = mozillaSslConfig;
    }

    public SslServerConfig getSslServerConfig() {
        return sslServerConfig;
    }

    public void setSslServerConfig(SslServerConfig sslServerConfig) {
        this.sslServerConfig = sslServerConfig;
    }
}
