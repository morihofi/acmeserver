package de.morihofi.acmeserver.types.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the configuration of a plugin together with the stored version.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginProperties {
    /** Version of the plugin when properties were last saved. */
    private String version;
    /** Map of property key to property definition. */
    @Builder.Default
    private Map<String, PluginProperty> properties = new HashMap<>();
}
