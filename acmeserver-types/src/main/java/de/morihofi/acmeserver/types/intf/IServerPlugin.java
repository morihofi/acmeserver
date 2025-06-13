package de.morihofi.acmeserver.types.intf;

import de.morihofi.acmeserver.types.plugin.PluginProperty;
import lombok.NonNull;

import java.util.Map;

/**
 * Interface for pluggable extensions that integrate with a running
 * {@link IServerInstance}. Implementations are loaded at runtime and may
 * register {@code EventSubscriber}s or provide additional functionality.
 */
public interface IServerPlugin {
    /**
     * Initializes the plugin with the given server instance.
     *
     * @param serverInstance server instance for registering listeners and accessing services
     */
    void initialize(@NonNull IServerInstance serverInstance,
                    @NonNull Map<String, PluginProperty> properties);

    /**
     * Unique identifier of the plugin.
     *
     * @return plugin ID
     */
    @NonNull
    String getPluginId();

    /**
     * Current version of the plugin.
     *
     * @return plugin version
     */
    @NonNull
    String getPluginVersion();

    /**
     * Invoked when the plugin version changed and configuration must be migrated.
     *
     * @param previousVersion previously stored version or {@code null} if first run
     * @param properties      modifiable property map for this plugin
     */
    default void propertyUpdate(String previousVersion, @NonNull Map<String, PluginProperty> properties) {
        // default no-op
    }

    /**
     * Checks whether all optional dependencies required by this plugin are available.
     * If this method returns {@code false}, the plugin will not be loaded.
     *
     * @return {@code true} if all dependencies are available
     */
    default boolean dependenciesAvailable() {
        return true;
    }
}
