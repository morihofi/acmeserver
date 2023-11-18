package de.morihofi.acmeserver.config;

import java.io.Serializable;
import java.lang.Integer;
import java.lang.String;
import java.util.List;

public class Config implements Serializable {
  private List<ProvisionerConfig> provisioner;

  public List<ProvisionerConfig> getProvisioner() {
    return this.provisioner;
  }

  public void setProvisioner(List<ProvisionerConfig> provisioner) {
    this.provisioner = provisioner;
  }


}
