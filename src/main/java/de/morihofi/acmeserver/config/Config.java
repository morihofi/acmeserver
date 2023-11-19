package de.morihofi.acmeserver.config;

import java.io.Serializable;
import java.lang.Integer;
import java.lang.String;
import java.util.List;

public class Config implements Serializable {
  private List<ProvisionerConfig> provisioner;
  private ServerConfig server;
  private DatabaseConfig database;
  private CertificateConfig rootCA;
  private EmailConfig emailSmtp;

  private ProxyConfig proxy;

  public List<ProvisionerConfig> getProvisioner() {
    return this.provisioner;
  }

  public void setProvisioner(List<ProvisionerConfig> provisioner) {
    this.provisioner = provisioner;
  }

  public ServerConfig getServer() {
    return server;
  }

  public DatabaseConfig getDatabase() {
    return database;
  }

  public CertificateConfig getRootCA() {
    return rootCA;
  }

  public void setServer(ServerConfig server) {
    this.server = server;
  }

  public void setDatabase(DatabaseConfig database) {
    this.database = database;
  }

  public void setRootCA(CertificateConfig rootCA) {
    this.rootCA = rootCA;
  }

  public EmailConfig getEmailSmtp() {
    return emailSmtp;
  }

  public void setEmailSmtp(EmailConfig emailSmtp) {
    this.emailSmtp = emailSmtp;
  }

  public ProxyConfig getProxy() {
    return proxy;
  }

  public void setProxy(ProxyConfig proxy) {
    this.proxy = proxy;
  }
}