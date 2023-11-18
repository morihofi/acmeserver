package de.morihofi.acmeserver.config;

import java.io.Serializable;
import java.lang.Integer;
import java.lang.String;
import java.util.List;

public class ServerConfig implements Serializable {
  private String dnsName;

  private Ports ports;

  public String getDnsName() {
    return dnsName;
  }

  public void setDnsName(String dnsName) {
    this.dnsName = dnsName;
  }

  public Ports getPorts() {
    return this.ports;
  }

  public void setPorts(Ports ports) {
    this.ports = ports;
  }


}
