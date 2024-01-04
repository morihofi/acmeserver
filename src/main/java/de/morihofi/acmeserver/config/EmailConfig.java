package de.morihofi.acmeserver.config;

import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;

/**
 * Represents configuration parameters for an email server, including host, port, encryption, username, password, and enabled status.
 */
public class EmailConfig implements Serializable {
  private String password;
  private String encryption;
  private Integer port;
  private String host;
  private Boolean enabled;
  private String username;

  /**
   * Get the password for the email server.
   * @return The email server password.
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * Set the password for the email server.
   * @param password The email server password to set.
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Get the encryption type for the email server.
   * @return The encryption type.
   */
  public String getEncryption() {
    return this.encryption;
  }

  /**
   * Set the encryption type for the email server.
   * @param encryption The encryption type to set.
   */
  public void setEncryption(String encryption) {
    this.encryption = encryption;
  }

  /**
   * Get the port number for the email server.
   * @return The port number.
   */
  public Integer getPort() {
    return this.port;
  }

  /**
   * Set the port number for the email server.
   * @param port The port number to set.
   */
  public void setPort(Integer port) {
    this.port = port;
  }

  /**
   * Get the host address of the email server.
   * @return The email server host address.
   */
  public String getHost() {
    return this.host;
  }

  /**
   * Set the host address of the email server.
   * @param host The email server host address to set.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Check if email server configuration is enabled.
   * @return True if enabled, false otherwise.
   */
  public Boolean getEnabled() {
    return this.enabled;
  }

  /**
   * Set the enabled status of email server configuration.
   * @param enabled The enabled status to set.
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Get the username for the email server.
   * @return The email server username.
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Set the username for the email server.
   * @param username The email server username to set.
   */
  public void setUsername(String username) {
    this.username = username;
  }
}
