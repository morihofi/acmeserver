package de.morihofi.acmeserver.types.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata describing a plugin. Parsed from {@code META-INF/acmeserver-plugin.json} inside the plugin JAR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginInfo {
    /** Human readable plugin author. */
    private String author;
    /** Short plugin description. */
    private String description;
    /** Path to an icon resource bundled in the plugin JAR. */
    private String icon;
    /** String based plugin version. */
    private String version;
    /** Fully qualified class name of the plugin implementation. */
    private String pluginClass;
}
