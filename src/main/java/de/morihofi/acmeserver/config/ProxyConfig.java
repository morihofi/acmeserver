package de.morihofi.acmeserver.config;

import de.morihofi.acmeserver.config.proxy.HttpChallenge;

import java.io.Serializable;

/**
 * Represents configuration parameters for a proxy, including HTTP challenge configuration.
 */
public class ProxyConfig implements Serializable {
  private HttpChallenge httpChallenge;

  /**
   * Get the HTTP challenge configuration for the proxy.
   * @return The HTTP challenge configuration.
   */
  public HttpChallenge getHttpChallenge() {
    return this.httpChallenge;
  }

  /**
   * Set the HTTP challenge configuration for the proxy.
   * @param httpChallenge The HTTP challenge configuration to set.
   */
  public void setHttpChallenge(HttpChallenge httpChallenge) {
    this.httpChallenge = httpChallenge;
  }
}
