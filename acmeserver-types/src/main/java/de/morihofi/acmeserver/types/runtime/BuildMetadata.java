package de.morihofi.acmeserver.types.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildMetadata {
    private String buildVersion;
    private String buildTime;
    private String gitCommit;
    private String gitClosestTagName;
}
