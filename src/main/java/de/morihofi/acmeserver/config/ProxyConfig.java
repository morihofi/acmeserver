package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.config.proxy.HttpChallenge;

import java.io.Serializable;

public class ProxyConfig implements Serializable {
  private HttpChallenge httpChallenge;

  public HttpChallenge getHttpChallenge() {
    return this.httpChallenge;
  }

  public void setHttpChallenge(HttpChallenge httpChallenge) {
    this.httpChallenge = httpChallenge;
  }

}
