package de.morihofi.acmeserver.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.lang.Boolean;
import java.lang.String;
import java.util.List;

/**
 * Represents configuration for domain name restrictions, including a list of required suffixes and an enabled flag.
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public class DomainNameRestrictionConfig implements Serializable {
  private List<String> mustEndWith;
  private Boolean enabled;

  /**
   * Get the list of required suffixes that domain names must end with.
   * @return The list of required suffixes.
   */
  public List<String> getMustEndWith() {
    return this.mustEndWith;
  }

  /**
   * Set the list of required suffixes that domain names must end with.
   * @param mustEndWith The list of required suffixes to set.
   */
  public void setMustEndWith(List<String> mustEndWith) {
    this.mustEndWith = mustEndWith;
  }

  /**
   * Check if domain name restrictions are enabled.
   * @return True if enabled, false otherwise.
   */
  public Boolean getEnabled() {
    return this.enabled;
  }

  /**
   * Set the enabled status of domain name restrictions.
   * @param enabled The enabled status to set.
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
