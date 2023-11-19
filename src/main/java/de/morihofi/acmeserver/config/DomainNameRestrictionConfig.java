package de.morihofi.acmeserver.config;

import java.io.Serializable;
import java.lang.Boolean;
import java.lang.String;
import java.util.List;

public class DomainNameRestrictionConfig implements Serializable {
  private List<String> mustEndWith;

  private Boolean enabled;

  public List<String> getMustEndWith() {
    return this.mustEndWith;
  }

  public void setMustEndWith(List<String> mustEndWith) {
    this.mustEndWith = mustEndWith;
  }

  public Boolean getEnabled() {
    return this.enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
