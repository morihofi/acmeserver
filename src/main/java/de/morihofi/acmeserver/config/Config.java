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

package de.morihofi.acmeserver.config;

import com.google.gson.annotations.SerializedName;
import de.morihofi.acmeserver.config.keyStoreHelpers.KeyStoreParams;
import de.morihofi.acmeserver.config.network.NetworkConfig;
import de.morihofi.acmeserver.configPreprocessor.annotation.ConfigurationField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a configuration for this ACME Server instance.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class Config implements Serializable {
    @SerializedName("$schema")
    private String jsonSchema;

    @ConfigurationField(name = "Web and API Server")
    private ServerConfig server;

    @ConfigurationField(name = "KeyStore", required = true)
    private KeyStoreParams keyStore;

    @ConfigurationField(name = "Database")
    private DatabaseConfig database;

    @ConfigurationField(name = "E-Mail SMTP")
    private EmailConfig emailSmtp = new EmailConfig();

    @ConfigurationField(name = "Root-CA")
    private CertificateConfig rootCA = new CertificateConfig();

    @ConfigurationField(name = "Provisioners")
    private List<ProvisionerConfig> provisioner = new ArrayList<>();

    @ConfigurationField(name = "Network settings")
    private NetworkConfig network = new NetworkConfig();

    /**
     * Get the list of provisioner configurations.
     *
     * @return The list of provisioner configurations.
     */
    public List<ProvisionerConfig> getProvisioner() {
        return this.provisioner;
    }

    /**
     * Set the list of provisioner configurations.
     *
     * @param provisioner The list of provisioner configurations to set.
     */
    public void setProvisioner(List<ProvisionerConfig> provisioner) {
        this.provisioner = provisioner;
    }

    /**
     * Get the key store parameters configuration.
     *
     * @return The key store parameters configuration.
     */
    public KeyStoreParams getKeyStore() {
        return keyStore;
    }

    /**
     * Set the key store parameters configuration.
     *
     * @param keyStore The key store parameters configuration to set.
     */
    public void setKeyStore(KeyStoreParams keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Get the server configuration.
     *
     * @return The server configuration.
     */
    public ServerConfig getServer() {
        return server;
    }

    /**
     * Set the server configuration.
     *
     * @param server The server configuration to set.
     */
    public void setServer(ServerConfig server) {
        this.server = server;
    }

    /**
     * Get the database configuration.
     *
     * @return The database configuration.
     */
    public DatabaseConfig getDatabase() {
        return database;
    }

    /**
     * Set the database configuration.
     *
     * @param database The database configuration to set.
     */
    public void setDatabase(DatabaseConfig database) {
        this.database = database;
    }

    /**
     * Get the root CA (Certificate Authority) configuration.
     *
     * @return The root CA configuration.
     */
    public CertificateConfig getRootCA() {
        return rootCA;
    }

    /**
     * Set the root CA (Certificate Authority) configuration.
     *
     * @param rootCA The root CA configuration to set.
     */
    public void setRootCA(CertificateConfig rootCA) {
        this.rootCA = rootCA;
    }

    /**
     * Get the email SMTP (Simple Mail Transfer Protocol) configuration.
     *
     * @return The email SMTP configuration.
     */
    public EmailConfig getEmailSmtp() {
        return emailSmtp;
    }

    /**
     * Set the email SMTP (Simple Mail Transfer Protocol) configuration.
     *
     * @param emailSmtp The email SMTP configuration to set.
     */
    public void setEmailSmtp(EmailConfig emailSmtp) {
        this.emailSmtp = emailSmtp;
    }



    public NetworkConfig getNetwork() {
        return network;
    }

    public void setNetwork(NetworkConfig network) {
        this.network = network;
    }

    public void saveConfig() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
