package de.morihofi.acmeserver.types.intf;

import lombok.NonNull;

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
    void initialize(@NonNull IServerInstance serverInstance);

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
