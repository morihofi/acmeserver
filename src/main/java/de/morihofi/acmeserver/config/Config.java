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
}
