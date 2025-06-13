package de.morihofi.acmeserver.core.plugin;

/**
 * Thrown when plugin configuration cannot be loaded or saved.
 */
public class PluginConfigException extends Exception {

    /** Creates a new exception with a message and cause. */
    public PluginConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
