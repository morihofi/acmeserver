package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.config.keyStoreHelpers.KeyStoreParams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a configuration for a system with various components.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class Config implements Serializable {
  private List<ProvisionerConfig> provisioner;
  private KeyStoreParams keyStore;
  private ServerConfig server;
  private DatabaseConfig database;
  private CertificateConfig rootCA;
  private EmailConfig emailSmtp;
  private ProxyConfig proxy;

  /**
   * Get the list of provisioner configurations.
   * @return The list of provisioner configurations.
   */
  public List<ProvisionerConfig> getProvisioner() {
    return this.provisioner;
  }

  /**
   * Set the list of provisioner configurations.
   * @param provisioner The list of provisioner configurations to set.
   */
  public void setProvisioner(List<ProvisionerConfig> provisioner) {
    this.provisioner = provisioner;
  }

  /**
   * Get the key store parameters configuration.
   * @return The key store parameters configuration.
   */
  public KeyStoreParams getKeyStore() {
    return keyStore;
  }

  /**
   * Set the key store parameters configuration.
   * @param keyStore The key store parameters configuration to set.
   */
  public void setKeyStore(KeyStoreParams keyStore) {
    this.keyStore = keyStore;
  }

  /**
   * Get the server configuration.
   * @return The server configuration.
   */
  public ServerConfig getServer() {
    return server;
  }

  /**
   * Get the database configuration.
   * @return The database configuration.
   */
  public DatabaseConfig getDatabase() {
    return database;
  }

  /**
   * Get the root CA (Certificate Authority) configuration.
   * @return The root CA configuration.
   */
  public CertificateConfig getRootCA() {
    return rootCA;
  }

  /**
   * Set the server configuration.
   * @param server The server configuration to set.
   */
  public void setServer(ServerConfig server) {
    this.server = server;
  }

  /**
   * Set the database configuration.
   * @param database The database configuration to set.
   */
  public void setDatabase(DatabaseConfig database) {
    this.database = database;
  }

  /**
   * Set the root CA (Certificate Authority) configuration.
   * @param rootCA The root CA configuration to set.
   */
  public void setRootCA(CertificateConfig rootCA) {
    this.rootCA = rootCA;
  }

  /**
   * Get the email SMTP (Simple Mail Transfer Protocol) configuration.
   * @return The email SMTP configuration.
   */
  public EmailConfig getEmailSmtp() {
    return emailSmtp;
  }

  /**
   * Set the email SMTP (Simple Mail Transfer Protocol) configuration.
   * @param emailSmtp The email SMTP configuration to set.
   */
  public void setEmailSmtp(EmailConfig emailSmtp) {
    this.emailSmtp = emailSmtp;
  }

  /**
   * Get the proxy configuration.
   * @return The proxy configuration.
   */
  public ProxyConfig getProxy() {
    return proxy;
  }

  /**
   * Set the proxy configuration.
   * @param proxy The proxy configuration to set.
   */
  public void setProxy(ProxyConfig proxy) {
    this.proxy = proxy;
  }
}
