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
    /**
     * Numeric version of the plugin when properties were last saved. Version
     * {@code 0} indicates that the plugin has not been initialized yet.
     */
    private long version;
    /** Map of property key to property definition. */
    @Builder.Default
    private Map<String, PluginProperty> properties = new HashMap<>();
}
